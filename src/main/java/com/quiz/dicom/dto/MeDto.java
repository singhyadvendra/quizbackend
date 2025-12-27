package com.quiz.dicom.dto;

import java.util.List;

public record MeDto(
        Long userId,
        String fullName,
        String email,
        List<IdentityDto> identities
) {
}
