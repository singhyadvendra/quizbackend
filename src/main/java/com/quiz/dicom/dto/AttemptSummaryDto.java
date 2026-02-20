package com.quiz.dicom.dto;

import java.time.OffsetDateTime;

public record AttemptSummaryDto(
        Long attemptId,
        Long quizId,
        String quizTitle,
        String status,
        String score,
        String totalPoints,
        OffsetDateTime createdAt,
        OffsetDateTime submittedAt
) {
}
