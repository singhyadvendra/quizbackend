package com.quiz.dicom.dto.admin;

import java.time.OffsetDateTime;

public record QuizAdminDto(
        Long id,
        String title,
        String description,
        boolean active,
        OffsetDateTime createdAt
) {}
