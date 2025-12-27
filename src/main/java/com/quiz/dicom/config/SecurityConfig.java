package com.quiz.dicom.config;

import com.quiz.dicom.service.OAuthUserUpsertService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.http.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;

import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.*;

import java.util.*;
import java.util.stream.Collectors;

@EnableConfigurationProperties(AppAdminProperties.class)
@Configuration
public class SecurityConfig {

    private static final String FRONTEND_ORIGIN = "http://localhost:5173";

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            OAuthUserUpsertService upsertService,
            AppAdminProperties adminProps
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/assets/**").permitAll()
                        .requestMatchers("/oauth2/**", "/login/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()

                        // Admin endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // All other API endpoints require authentication
                        .requestMatchers("/api/**").authenticated()

                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo
                                // OIDC providers (Google, LinkedIn with openid/profile/email)
                                .oidcUserService(oidcUserService(adminProps))
                                // OAuth2 providers (GitHub)
                                .userService(oauth2UserService(adminProps))
                        )
                        .successHandler((request, response, authentication) -> {
                            upsertService.upsertFromAuthentication(authentication);

                            boolean isAdmin = authentication.getAuthorities().stream()
                                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

                            response.sendRedirect(FRONTEND_ORIGIN + (isAdmin ? "/admin" : "/quiz"));
                        })

                )
                .logout(logout -> logout
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((req, res, auth) -> res.sendRedirect(FRONTEND_ORIGIN + "/"))
                );

        return http.build();
    }

    /**
     * OIDC user service (Google, LinkedIn OIDC): reads email from OIDC claims and assigns ROLE_ADMIN
     * if email is present in app.admin.emails.
     */
    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService(AppAdminProperties adminProps) {
        OidcUserService delegate = new OidcUserService();

        final Set<String> adminEmails = normalizeAdminEmails(adminProps);

        return (userRequest) -> {
            OidcUser oidcUser = delegate.loadUser(userRequest);

            Set<GrantedAuthority> mapped = new HashSet<>(oidcUser.getAuthorities());
            mapped.add(new SimpleGrantedAuthority("ROLE_USER"));

            String email = oidcUser.getEmail(); // standard OIDC claim accessor
            if (email != null && adminEmails.contains(email.trim().toLowerCase())) {
                mapped.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }

            // Keep the same tokens and userinfo, just augment authorities
            return new DefaultOidcUser(mapped, oidcUser.getIdToken(), oidcUser.getUserInfo(), "sub");
        };
    }

    /**
     * OAuth2 user service (GitHub): attempts to read email from user attributes; if missing,
     * calls GitHub /user/emails (requires scope: user:email) and uses verified primary email.
     */
    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService(AppAdminProperties adminProps) {
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

        final Set<String> adminEmails = normalizeAdminEmails(adminProps);

        return (userRequest) -> {
            OAuth2User user = delegate.loadUser(userRequest);

            Set<GrantedAuthority> mapped = new HashSet<>(user.getAuthorities());
            mapped.add(new SimpleGrantedAuthority("ROLE_USER"));

            String registrationId = userRequest.getClientRegistration().getRegistrationId();
            String email = null;

            // Try direct attribute first
            Object emailObj = user.getAttributes().get("email");
            if (emailObj != null) email = emailObj.toString();

            // GitHub often returns null email unless it's public; fetch from /user/emails
            if ("github".equalsIgnoreCase(registrationId) && (email == null || email.isBlank())) {
                email = fetchGithubPrimaryVerifiedEmail(userRequest.getAccessToken());
            }

            if (email != null && adminEmails.contains(email.trim().toLowerCase())) {
                mapped.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }

            String nameAttrKey = userRequest.getClientRegistration()
                    .getProviderDetails()
                    .getUserInfoEndpoint()
                    .getUserNameAttributeName();

            // Defensive fallback if not set (GitHub typically uses "id")
            if (nameAttrKey == null || nameAttrKey.isBlank()) {
                nameAttrKey = "id";
            }

            return new DefaultOAuth2User(mapped, user.getAttributes(), nameAttrKey);
        };
    }

    private Set<String> normalizeAdminEmails(AppAdminProperties adminProps) {
        return adminProps.getEmails().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    private String fetchGithubPrimaryVerifiedEmail(OAuth2AccessToken accessToken) {
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

            for (Object item : resp.getBody()) {
                if (!(item instanceof Map<?, ?> m)) continue;

                Object primary = m.get("primary");
                Object verified = m.get("verified");
                Object email = m.get("email");

                if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified) && email != null) {
                    return email.toString();
                }
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(FRONTEND_ORIGIN));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
