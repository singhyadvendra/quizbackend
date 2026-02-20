package com.quiz.dicom.dto.admin;

import jakarta.validation.constraints.*;

public record CreateQuizRequestDto(

        @NotBlank
        @Size(max = 200)
        String title,

        @Size(max = 2000)
        String description,

        boolean active
) {}
