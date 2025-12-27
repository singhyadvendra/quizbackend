package com.quiz.dicom.mapper;

import com.quiz.dicom.domain.Attempt;
import com.quiz.dicom.dto.AttemptResultDto;

public final class AttemptMapper {

    private AttemptMapper() {}

    public static AttemptResultDto toResultDto(Attempt attempt) {
        return new AttemptResultDto(
                attempt.getId(),
                attempt.getQuiz().getId(),
                attempt.getStatus() == null ? null : attempt.getStatus().name(),
                attempt.getScore() == null ? "0" : attempt.getScore().toPlainString(),
                attempt.getTotalPoints() == null ? "0" : attempt.getTotalPoints().toPlainString(),
                attempt.getStartedAt(),
                attempt.getSubmittedAt()
        );
    }
}
