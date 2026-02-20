package com.quiz.dicom.dto;

import java.time.OffsetDateTime;

public record StartAttemptResponseDto(
        Long attemptId,
        Long quizId,
        OffsetDateTime startedAt
) {
}
