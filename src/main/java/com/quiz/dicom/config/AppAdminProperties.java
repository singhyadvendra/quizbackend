package com.quiz.dicom.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.admin")
public class AppAdminProperties {

    private List<String> emails = List.of();

    public List<String> getEmails() {
        return emails;
    }
    // MUST be a plain setter in Boot 4
    public void setEmails(List<String> emails) {
        this.emails = emails;
    }

    /**
     * Normalized admin emails (lowercase, trimmed)
     */
    public List<String> normalizedEmails() {
        return emails.stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .toList();
    }
}
