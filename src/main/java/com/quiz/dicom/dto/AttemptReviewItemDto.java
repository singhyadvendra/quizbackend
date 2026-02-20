// com.quiz.dicom.dto.AttemptReviewItemDto.java
package com.quiz.dicom.dto;

import com.quiz.dicom.domain.QuestionType;

import java.util.List;

public record AttemptReviewItemDto(
        Long questionId,
        int questionNo,
        QuestionType type,
        String text,
        String achievedScore,   // NEW
        String maxScore,        // NEW
        boolean required,
        List<OptionDto> options,
        List<Long> selectedOptionIds,
        List<Long> correctOptionIds,
        boolean isCorrect
) {}
