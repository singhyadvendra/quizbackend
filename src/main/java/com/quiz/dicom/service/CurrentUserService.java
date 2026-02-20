package com.quiz.dicom.service;

import com.quiz.dicom.domain.UserIdentity;
import com.quiz.dicom.dto.IdentityDto;
import com.quiz.dicom.dto.MeDto;
import com.quiz.dicom.repository.UserIdentityRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CurrentUserService {

    private final UserIdentityRepository userIdentityRepository;

    public CurrentUserService(UserIdentityRepository userIdentityRepository) {
        this.userIdentityRepository = userIdentityRepository;
    }

    /**
     * Returns the current logged-in user's profile, resolved via (provider, sub).
     * Assumes you already upsert user_identity on login (via success handler).
     */
    @Transactional(readOnly = true)
    public MeDto me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof OAuth2AuthenticationToken token)) {
            throw new IllegalStateException("Not an OAuth2 authenticated session");
        }

        String provider = token.getAuthorizedClientRegistrationId();
        Object principal = token.getPrincipal();
        String sub;

        if (principal instanceof OidcUser oidc) {
            sub = oidc.getSubject(); // Google/LinkedIn use 'sub'
        } else if (principal instanceof OAuth2User oauth2) {
            // GitHub: Extract "id" to match what the Upsert service saved
            sub = Optional.ofNullable(oauth2.getAttribute("id"))
                    .map(Object::toString)
                    .orElse(oauth2.getName());
        } else {
            sub = auth.getName();
        }

        UserIdentity identity = userIdentityRepository
                .findByProviderAndProviderSubject(provider, sub)
                .orElseThrow(() -> new RuntimeException(
                        "User identity not found for " + provider + " [" + sub + "]. Check database for subject mismatch."
                ));

        var user = identity.getUser();
        if (user == null) {
            throw new IllegalStateException("Identity found but not linked to a primary AppUser record.");
        }

        // Fetch all identities (e.g., if they have both GitHub and LinkedIn linked)
        List<IdentityDto> identities = userIdentityRepository.findAllByUserId(user.getId())
                .stream()
                .map(i -> new IdentityDto(
                        i.getProvider(),
                        i.getProviderSubject(),
                        i.getDisplayName(),
                        i.getEmail(),
                        i.getEmailVerified(),
                        i.getPictureUrl(),
                        i.getLastLoginAt()
                ))
                .toList();

        return new MeDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                identities
        );
    }

    /**
     * Convenience method for other services/controllers that only need internal userId.
     */
    @Transactional(readOnly = true)
    public Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof OAuth2AuthenticationToken token)) {
            throw new IllegalStateException("Not an OAuth2 authenticated session");
        }

        String provider = token.getAuthorizedClientRegistrationId();
        Object principal = token.getPrincipal();
        String sub;

        if (principal instanceof OidcUser oidc) {
            sub = oidc.getSubject();
        } else if (principal instanceof OAuth2User oauth2) {
            sub = Optional.ofNullable(oauth2.getAttribute("id"))
                    .map(Object::toString)
                    .orElse(oauth2.getName());
        } else {
            sub = auth.getName();
        }

        return userIdentityRepository.findByProviderAndProviderSubject(provider, sub)
                .map(UserIdentity::getUser)
                .map(u -> u.getId())
                .orElseThrow(() -> new IllegalStateException("No mapped user for " + provider + " subject: " + sub));
    }
}
