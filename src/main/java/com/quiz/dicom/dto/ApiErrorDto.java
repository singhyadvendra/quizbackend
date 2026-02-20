package com.quiz.dicom.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiErrorDto(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldViolationDto> violations
) {}
