package com.quiz.dicom.service;

import com.quiz.dicom.dto.MeDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminAuthorizationService {

    private final CurrentUserService currentUserService;
    private final List<String> adminEmails;

    public AdminAuthorizationService(CurrentUserService currentUserService,
                                     @Value("${app.admin.emails:}") List<String> adminEmails) {
        this.currentUserService = currentUserService;
        this.adminEmails = adminEmails == null ? List.of() : adminEmails;
    }

    public void requireAdmin() {
        MeDto me = currentUserService.me();
        String email = me.email();
        if (email == null || adminEmails.stream().noneMatch(a -> a.equalsIgnoreCase(email))) {
            throw new AccessDeniedException("Admin access required");
        }
    }
}
