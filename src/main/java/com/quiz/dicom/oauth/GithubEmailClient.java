package com.quiz.dicom.oauth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

public class GithubEmailClient {

    private static final Logger log = LoggerFactory.getLogger(GithubEmailClient.class);
    private final RestTemplate restTemplate;

    public GithubEmailClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    public String fetchPrimaryVerifiedEmail(OAuth2AccessToken token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token.getTokenValue());
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            ResponseEntity<List> response = restTemplate.exchange(
                    "https://api.github.com/user/emails",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    List.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return null;
            }

            for (Object o : response.getBody()) {
                Map<String, Object> m = (Map<String, Object>) o;
                if (Boolean.TRUE.equals(m.get("primary"))
                        && Boolean.TRUE.equals(m.get("verified"))) {
                    return m.get("email").toString();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch GitHub email", e);
        }
        return null;
    }
}
