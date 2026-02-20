package com.quiz.dicom.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record QuizDetailDto(
        Long id,
        String title,
        String description,
        boolean active,
        OffsetDateTime createdAt,
        List<QuestionDto> questions
) {
}
