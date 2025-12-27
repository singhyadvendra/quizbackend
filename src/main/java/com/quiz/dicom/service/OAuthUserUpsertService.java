package com.quiz.dicom.service;

import com.quiz.dicom.domain.AppUser;
import com.quiz.dicom.domain.UserIdentity;
import com.quiz.dicom.repository.AppUserRepository;
import com.quiz.dicom.repository.UserIdentityRepository;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class OAuthUserUpsertService {

    private final AppUserRepository appUserRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;

    public OAuthUserUpsertService(AppUserRepository appUserRepository,
                                  UserIdentityRepository userIdentityRepository,
                                  OAuth2AuthorizedClientService authorizedClientService) {
        this.appUserRepository = appUserRepository;
        this.userIdentityRepository = userIdentityRepository;
        this.authorizedClientService = authorizedClientService;
    }

    @Transactional
    public void upsertFromAuthentication(Authentication authentication) {
        if (!(authentication instanceof OAuth2AuthenticationToken token)) {
            return;
        }

        String provider = token.getAuthorizedClientRegistrationId(); // google / linkedin / github
        Object principal = token.getPrincipal();

        ExtractedIdentity extracted = extractIdentity(provider, token, principal);

        // 1) Find identity by (provider, providerSubject)
        Optional<UserIdentity> existingIdentity =
                userIdentityRepository.findByProviderAndProviderSubject(provider, extracted.providerSubject());

        if (existingIdentity.isPresent()) {
            UserIdentity identity = existingIdentity.get();
            identity.setEmail(extracted.email());
            identity.setEmailVerified(extracted.emailVerified());
            identity.setDisplayName(extracted.displayName());
            identity.setPictureUrl(extracted.pictureUrl());
            identity.setLastLoginAt(OffsetDateTime.now());
            userIdentityRepository.save(identity);

            // Optionally sync AppUser display fields too
            AppUser user = identity.getUser();
            if (user != null) {
                if (user.getFullName() == null || user.getFullName().isBlank()) user.setFullName(extracted.displayName());
                if (user.getEmail() == null || user.getEmail().isBlank()) user.setEmail(extracted.email());
                appUserRepository.save(user);
            }
            return;
        }

        // 2) Create/link AppUser
        // Policy: if verified email exists and matches an existing user, link to it; else create a new user.
        AppUser userToUse = null;
        if (extracted.email() != null && !extracted.email().isBlank() && Boolean.TRUE.equals(extracted.emailVerified())) {
            userToUse = appUserRepository.findByEmail(extracted.email()).orElse(null);
        }
        if (userToUse == null) {
            AppUser newUser = new AppUser();
            newUser.setFullName(extracted.displayName());
            newUser.setEmail(extracted.email());
            newUser.setCreatedAt(OffsetDateTime.now());
            userToUse = appUserRepository.save(newUser);
        }

        // 3) Insert identity
        UserIdentity identity = new UserIdentity();
        identity.setUser(userToUse);
        identity.setProvider(provider);
        identity.setProviderSubject(extracted.providerSubject());
        identity.setEmail(extracted.email());
        identity.setEmailVerified(extracted.emailVerified());
        identity.setDisplayName(extracted.displayName());
        identity.setPictureUrl(extracted.pictureUrl());
        identity.setCreatedAt(OffsetDateTime.now());
        identity.setLastLoginAt(OffsetDateTime.now());

        userIdentityRepository.save(identity);
    }

    private ExtractedIdentity extractIdentity(String provider, OAuth2AuthenticationToken token, Object principal) {
        // ---- OIDC providers: Google, LinkedIn OIDC ----
        if (principal instanceof OidcUser oidcUser) {
            Map<String, Object> claims = oidcUser.getClaims();

            String sub = asString(claims.get("sub"));
            if (sub == null || sub.isBlank()) {
                throw new IllegalStateException("Missing OIDC 'sub' claim for provider=" + provider);
            }

            String email = asString(claims.get("email"));
            Boolean emailVerified = (claims.get("email_verified") instanceof Boolean b) ? b : null;
            String name = asString(Optional.ofNullable(claims.get("name")).orElse(claims.get("given_name")));
            String picture = asString(claims.get("picture"));

            return new ExtractedIdentity(sub, email, emailVerified, name, picture);
        }

        // ---- OAuth2 providers: GitHub (and any other non-OIDC) ----
        if (principal instanceof OAuth2User oauth2User) {
            Map<String, Object> attrs = oauth2User.getAttributes();

            // Subject: for GitHub use "id" (numeric). Fallback to oauth2User.getName().
            String subject = firstNonBlank(
                    asString(attrs.get("id")),
                    oauth2User.getName()
            );
            if (subject == null || subject.isBlank()) {
                throw new IllegalStateException("Missing OAuth2 subject for provider=" + provider);
            }

            String email = asString(attrs.get("email")); // GitHub often null
            Boolean emailVerified = null; // GitHub /user doesn't provide verified flag here
            String name = firstNonBlank(asString(attrs.get("name")), asString(attrs.get("login")));
            String picture = asString(attrs.get("avatar_url"));

            // If GitHub email is null, fetch from /user/emails (requires scope user:email)
            if ("github".equalsIgnoreCase(provider) && (email == null || email.isBlank())) {
                GithubEmailResult r = fetchGithubPrimaryVerifiedEmail(token);
                if (r != null) {
                    email = r.email();
                    emailVerified = r.verified();
                }
            }

            return new ExtractedIdentity(subject, email, emailVerified, name, picture);
        }

        throw new IllegalStateException("Unsupported principal type: " + principal.getClass().getName()
                + " for provider=" + provider);
    }

    private GithubEmailResult fetchGithubPrimaryVerifiedEmail(OAuth2AuthenticationToken token) {
        OAuth2AuthorizedClient client =
                authorizedClientService.loadAuthorizedClient(token.getAuthorizedClientRegistrationId(), token.getName());
        if (client == null) return null;

        OAuth2AccessToken accessToken = client.getAccessToken();
        if (accessToken == null) return null;

        try {
            RestTemplate rt = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken.getTokenValue());
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Void> req = new HttpEntity<>(headers);

            ResponseEntity<List> resp = rt.exchange(
                    "https://api.github.com/user/emails",
                    HttpMethod.GET,
                    req,
                    List.class
            );

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) return null;

            // Find primary & verified email first
            for (Object item : resp.getBody()) {
                if (!(item instanceof Map<?, ?> m)) continue;

                Object primary = m.get("primary");
                Object verified = m.get("verified");
                Object email = m.get("email");

                if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified) && email != null) {
                    return new GithubEmailResult(email.toString(), true);
                }
            }

            // Fallback: any primary email (even if not verified)
            for (Object item : resp.getBody()) {
                if (!(item instanceof Map<?, ?> m)) continue;

                Object primary = m.get("primary");
                Object verified = m.get("verified");
                Object email = m.get("email");

                if (Boolean.TRUE.equals(primary) && email != null) {
                    return new GithubEmailResult(email.toString(), Boolean.TRUE.equals(verified));
                }
            }

            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    private record ExtractedIdentity(
            String providerSubject,
            String email,
            Boolean emailVerified,
            String displayName,
            String pictureUrl
    ) { }

    private record GithubEmailResult(String email, boolean verified) { }
}
