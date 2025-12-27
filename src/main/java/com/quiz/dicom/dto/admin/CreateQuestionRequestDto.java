package com.quiz.dicom.dto.admin;

import com.quiz.dicom.domain.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateQuestionRequestDto(
        @NotNull Integer questionNo,
        @NotNull QuestionType type,
        @NotBlank String text,
        @NotNull BigDecimal points,
        boolean required
) {}
