package com.quiz.dicom.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record CreateQuizRequestDto(
        @NotBlank String title,
        String description,
        boolean active
) {}
