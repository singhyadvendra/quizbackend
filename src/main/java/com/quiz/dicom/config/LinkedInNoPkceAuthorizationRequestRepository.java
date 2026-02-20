package com.quiz.dicom.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;

import java.util.LinkedHashMap;
import java.util.Map;

public final class LinkedInNoPkceAuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private final HttpSessionOAuth2AuthorizationRequestRepository delegate =
            new HttpSessionOAuth2AuthorizationRequestRepository();

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        OAuth2AuthorizationRequest req = delegate.loadAuthorizationRequest(request);
        return stripPkceIfLinkedIn(request, req);
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        OAuth2AuthorizationRequest stripped = stripPkceIfLinkedIn(request, authorizationRequest);
        delegate.saveAuthorizationRequest(stripped, request, response);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        OAuth2AuthorizationRequest req = delegate.removeAuthorizationRequest(request, response);
        return stripPkceIfLinkedIn(request, req);
    }

    private OAuth2AuthorizationRequest stripPkceIfLinkedIn(HttpServletRequest http, OAuth2AuthorizationRequest req) {
        if (req == null) return null;

        String uri = http.getRequestURI();

        // Covers both:
        // 1) /oauth2/authorization/linkedin
        // 2) /login/oauth2/code/linkedin (callback)
        boolean isLinkedIn =
                (uri != null && (uri.endsWith("/linkedin") || uri.contains("/linkedin")));

        if (!isLinkedIn) return req;

        Map<String, Object> additional = new LinkedHashMap<>(req.getAdditionalParameters());
        additional.remove(PkceParameterNames.CODE_CHALLENGE);
        additional.remove(PkceParameterNames.CODE_CHALLENGE_METHOD);

        Map<String, Object> attrs = new LinkedHashMap<>(req.getAttributes());
        attrs.remove(PkceParameterNames.CODE_VERIFIER);

        return OAuth2AuthorizationRequest.from(req)
                .additionalParameters(additional)
                .attributes(attrs)
                .build();
    }
}
