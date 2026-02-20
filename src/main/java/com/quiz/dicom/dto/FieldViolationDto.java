package com.quiz.dicom.dto;

public record FieldViolationDto(
        String field,
        String message
) {}
