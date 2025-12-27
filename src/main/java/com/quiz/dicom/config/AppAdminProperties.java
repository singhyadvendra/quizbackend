package com.quiz.dicom.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.admin")
public class AppAdminProperties {
    /**
     * List of emails that should be granted ROLE_ADMIN after OAuth login.
     */
    private List<String> emails = new ArrayList<>();

    public List<String> getEmails() {
        return emails;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
    }
}
