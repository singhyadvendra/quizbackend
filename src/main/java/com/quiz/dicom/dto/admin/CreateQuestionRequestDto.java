package com.quiz.dicom.dto.admin;

import com.quiz.dicom.domain.QuestionType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateQuestionRequestDto(

        @NotNull
        @Positive
        Integer questionNo,

        @NotNull
        QuestionType type,

        @NotBlank
        @Size(max = 1000)
        String text,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        @Digits(integer = 6, fraction = 2)
        BigDecimal points,

        boolean required
) {}
