package com.quiz.dicom.dto.admin;

import java.time.OffsetDateTime;

public record OptionAdminDto(
        Long id,
        Long questionId,
        Integer optionNo,
        String text,
        boolean correct,
        OffsetDateTime createdAt
) {}
