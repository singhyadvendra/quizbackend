// com.quiz.dicom.dto.AttemptReviewDto.java
package com.quiz.dicom.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record AttemptReviewDto(
        Long attemptId,
        Long quizId,
        String quizTitle,
        String status,
        String score,
        String totalPoints,
        OffsetDateTime startedAt,
        OffsetDateTime submittedAt,
        List<AttemptReviewItemDto> items
) {}
