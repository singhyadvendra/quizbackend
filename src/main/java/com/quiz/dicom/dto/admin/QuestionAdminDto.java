package com.quiz.dicom.dto.admin;

import com.quiz.dicom.domain.QuestionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record QuestionAdminDto(
        Long id,
        Long quizId,
        Integer questionNo,
        QuestionType type,
        String text,
        BigDecimal points,
        boolean required,
        OffsetDateTime createdAt
) {}
