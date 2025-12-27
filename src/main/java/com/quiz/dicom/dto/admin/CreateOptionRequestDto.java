package com.quiz.dicom.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOptionRequestDto(
        @NotNull Integer optionNo,
        @NotBlank String text,
        boolean correct
) {}
