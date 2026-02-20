package com.quiz.dicom.dto;

import java.time.OffsetDateTime;

public record IdentityDto(
        String provider,           // "google" / "linkedin"
        String providerSubject,    // OIDC "sub"
        String displayName,
        String email,
        Boolean emailVerified,
        String pictureUrl,
        OffsetDateTime lastLoginAt
) {
}
