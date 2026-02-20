package com.quiz.dicom.dto;

import java.util.List;

public record LeaderboardResponseDto(
        Long quizId,
        List<LeaderboardEntryDto> entries
) {
}
