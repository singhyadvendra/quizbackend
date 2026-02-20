package com.quiz.dicom.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.LinkedHashMap;
import java.util.Map;

public final class LinkedInNoPkceAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    public LinkedInNoPkceAuthorizationRequestResolver(
            ClientRegistrationRepository repo,
            String authorizationRequestBaseUri
    ) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(repo, authorizationRequestBaseUri);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest req = delegate.resolve(request);
        return stripPkceIfLinkedIn(request, req);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest req = delegate.resolve(request, clientRegistrationId);
        return stripPkceIfLinkedIn(request, req);
    }

    private OAuth2AuthorizationRequest stripPkceIfLinkedIn(HttpServletRequest http, OAuth2AuthorizationRequest req) {
        if (req == null) return null;

        // Identify LinkedIn by path or by registration id path segment
        // (works for /oauth2/authorization/linkedin)
        String uri = http.getRequestURI();
        boolean isLinkedIn = uri != null && uri.endsWith("/linkedin");
        if (!isLinkedIn) return req;

        // Remove code_challenge + method (sent on the auth redirect)
        Map<String, Object> additional = new LinkedHashMap<>(req.getAdditionalParameters());
        additional.remove(PkceParameterNames.CODE_CHALLENGE);
        additional.remove(PkceParameterNames.CODE_CHALLENGE_METHOD);

        // Remove code_verifier (used later when exchanging the code for tokens)
        Map<String, Object> attrs = new LinkedHashMap<>(req.getAttributes());
        attrs.remove(PkceParameterNames.CODE_VERIFIER);

        return OAuth2AuthorizationRequest.from(req)
                .additionalParameters(additional)
                .attributes(attrs)
                .build();
    }
}
