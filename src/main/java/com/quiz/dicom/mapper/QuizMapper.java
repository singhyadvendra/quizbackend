package com.quiz.dicom.mapper;

import com.quiz.dicom.domain.Quiz;
import com.quiz.dicom.dto.QuizSummaryDto;

public final class QuizMapper {

    private QuizMapper() {}

    public static QuizSummaryDto toSummaryDto(Quiz quiz) {
        return new QuizSummaryDto(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.isActive(),
                quiz.getCreatedAt()
        );
    }
}
