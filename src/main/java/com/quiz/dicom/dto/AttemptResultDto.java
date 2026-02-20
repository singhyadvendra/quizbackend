package com.quiz.dicom.dto;

import java.time.OffsetDateTime;

public record AttemptResultDto(
        Long attemptId,
        Long quizId,
        String status,
        String score,
        String totalPoints,
        OffsetDateTime startedAt,
        OffsetDateTime submittedAt
) {}
