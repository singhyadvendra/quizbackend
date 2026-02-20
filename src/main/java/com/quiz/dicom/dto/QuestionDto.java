package com.quiz.dicom.dto;

import java.util.List;

public record QuestionDto(
        Long id,
        int questionNo,
        String type,        // "SINGLE" or "MULTI"
        String text,
        String points,      // BigDecimal serialized as string
        boolean required,
        List<OptionDto> options
) {}
