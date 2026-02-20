package com.quiz.dicom.dto;

import java.time.OffsetDateTime;

public record LeaderboardEntryDto(
        long rank,
        Long userId,
        String displayName,
        String score,
        OffsetDateTime submittedAt
) {
}
