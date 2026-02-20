package com.quiz.dicom.oauth;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public final class OidcUserServiceConfig {

    private OidcUserServiceConfig() {
        // prevent instantiation
    }

    /**
     * Factory method used by SecurityConfig
     */
    public static OAuth2UserService<OidcUserRequest, OidcUser> create(
            OAuthRoleMapper roleMapper
    ) {
        OidcUserService delegate = new OidcUserService();

        return (OidcUserRequest request) -> {
            OidcUser user = delegate.loadUser(request);

            return new DefaultOidcUser(
                    roleMapper.mapAuthorities(user.getAuthorities(), user.getEmail()),
                    user.getIdToken(),
                    user.getUserInfo(),
                    "sub"
            );
        };
    }
}
