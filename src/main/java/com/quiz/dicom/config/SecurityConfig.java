package com.quiz.dicom.config;

import com.quiz.dicom.oauth.GithubEmailClient;
import com.quiz.dicom.oauth.OAuth2UserServiceConfig;
import com.quiz.dicom.oauth.OAuthRoleMapper;
import com.quiz.dicom.oauth.OidcUserServiceConfig;
import com.quiz.dicom.service.OAuthUserUpsertService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
@Configuration
@EnableConfigurationProperties({
        AppAdminProperties.class,
        FrontendProperties.class
})
public class SecurityConfig {

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    OAuthRoleMapper roleMapper(AppAdminProperties props) {
        return new OAuthRoleMapper(props);
    }

    @Bean
    GithubEmailClient githubEmailClient(RestTemplate rt) {
        return new GithubEmailClient(rt);
    }

    @Bean
    SecurityFilterChain filterChain(
            HttpSecurity http,
            OAuthUserUpsertService upsertService,
            OAuthRoleMapper roleMapper,
            GithubEmailClient githubClient,
            FrontendProperties frontend,
            ClientRegistrationRepository repo // Inject Repo instead of the resolver
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource(frontend)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/index.html", "/favicon.ico", "/error",
                                "/login/**", "/oauth2/**",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html" // OpenAPI paths
                        ).permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                )
                .oauth2Login(oauth -> oauth
                        // Reference your beans defined below directly
                        .authorizationEndpoint(a -> a.authorizationRequestResolver(authorizationRequestResolver(repo)))
                        .tokenEndpoint(t -> t.accessTokenResponseClient(linkedinNoPkceTokenClient()))
                        .userInfoEndpoint(u -> u
                                .oidcUserService(OidcUserServiceConfig.create(roleMapper))
                                .userService(OAuth2UserServiceConfig.create(roleMapper, githubClient))
                        )
                        .successHandler((req, res, auth) -> {
                            upsertService.upsertFromAuthentication(auth);
                            boolean isAdmin = auth.getAuthorities().stream()
                                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                            res.sendRedirect(frontend.origin() + (isAdmin ? "/admin" : "/quiz"));
                        })
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(FrontendProperties frontend) {
        CorsConfiguration configuration = new CorsConfiguration();

        // This pulls "http://localhost:5173" from your application.yml
        configuration.setAllowedOrigins(List.of(frontend.origin()));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));

        // Essential for keeping the user logged in via JSESSIONID cookie
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }



    @Bean
    OAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ClientRegistrationRepository repo
    ) {
        DefaultOAuth2AuthorizationRequestResolver delegate =
                new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");

        return new OAuth2AuthorizationRequestResolver() {
            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
                return stripPkceIfLinkedIn(request, delegate.resolve(request));
            }

            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
                return stripPkceIfLinkedIn(clientRegistrationId, delegate.resolve(request, clientRegistrationId));
            }

            private OAuth2AuthorizationRequest stripPkceIfLinkedIn(HttpServletRequest request, OAuth2AuthorizationRequest ar) {
                if (ar == null) return null;
                String uri = request.getRequestURI(); // e.g. /oauth2/authorization/linkedin
                String regId = uri.startsWith("/oauth2/authorization/")
                        ? uri.substring("/oauth2/authorization/".length())
                        : null;
                return stripPkceIfLinkedIn(regId, ar);
            }

            private OAuth2AuthorizationRequest stripPkceIfLinkedIn(String regId, OAuth2AuthorizationRequest ar) {
                if (ar == null) return null;
                if (!"linkedin".equals(regId)) return ar;

                return OAuth2AuthorizationRequest.from(ar)
                        .additionalParameters(params -> {
                            params.remove(PkceParameterNames.CODE_CHALLENGE);
                            params.remove(PkceParameterNames.CODE_CHALLENGE_METHOD);
                            params.remove(OidcParameterNames.NONCE);
                        })
                        .attributes(attrs -> {
                            // remove stored nonce so Spring doesn't expect it later
                            attrs.remove(OidcParameterNames.NONCE);
                        })
                        .build();
            }
        };
    }


    @Bean
    OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> linkedinNoPkceTokenClient() {
        RestClientAuthorizationCodeTokenResponseClient client =
                new RestClientAuthorizationCodeTokenResponseClient();

        client.setParametersCustomizer(params -> {
            // remove PKCE verifier
            params.remove(PkceParameterNames.CODE_VERIFIER);
        });

        return request -> {
            // apply only to LinkedIn; leave others untouched
            if (!"linkedin".equals(request.getClientRegistration().getRegistrationId())) {
                return new RestClientAuthorizationCodeTokenResponseClient().getTokenResponse(request);
            }
            return client.getTokenResponse(request);
        };
    }

}
