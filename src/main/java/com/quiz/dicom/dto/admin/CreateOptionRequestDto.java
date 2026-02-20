package com.quiz.dicom.dto.admin;

import jakarta.validation.constraints.*;

public record CreateOptionRequestDto(

        @NotNull
        @Positive
        Integer optionNo,

        @NotBlank
        @Size(max = 500)
        String text,

        boolean correct
) {}
