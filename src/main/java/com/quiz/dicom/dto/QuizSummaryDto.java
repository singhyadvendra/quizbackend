package com.quiz.dicom.dto;

import java.time.OffsetDateTime;

public record QuizSummaryDto(
        Long id,
        String title,
        String description,
        boolean active,
        OffsetDateTime createdAt,
        String topicCode,
        String topicName
) {}

