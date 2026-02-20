package com.quiz.dicom.oauth;

import org.springframework.security.oauth2.client.userinfo.*;
import org.springframework.security.oauth2.core.user.*;

public class OAuth2UserServiceConfig {

    public static OAuth2UserService<OAuth2UserRequest, OAuth2User> create(
            OAuthRoleMapper roleMapper,
            GithubEmailClient githubClient
    ) {
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

        return (OAuth2UserRequest request) -> {
            OAuth2User user = delegate.loadUser(request);

            String email = (String) user.getAttributes().get("email");

            if ("github".equalsIgnoreCase(
                    request.getClientRegistration().getRegistrationId())
                    && (email == null || email.isBlank())) {
                email = githubClient.fetchPrimaryVerifiedEmail(request.getAccessToken());
            }

            String nameKey = request.getClientRegistration()
                    .getProviderDetails()
                    .getUserInfoEndpoint()
                    .getUserNameAttributeName();

            if (nameKey == null || nameKey.isBlank()) {
                nameKey = "id";
            }

            return new DefaultOAuth2User(
                    roleMapper.mapAuthorities(user.getAuthorities(), email),
                    user.getAttributes(),
                    nameKey
            );
        };
    }
}
