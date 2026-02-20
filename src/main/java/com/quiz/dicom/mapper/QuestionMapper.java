package com.quiz.dicom.mapper;

import com.quiz.dicom.domain.Question;
import com.quiz.dicom.domain.QuestionOption;
import com.quiz.dicom.domain.ScoringMode;
import com.quiz.dicom.dto.OptionDto;
import com.quiz.dicom.dto.QuestionDto;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public final class QuestionMapper {

    private QuestionMapper() {}

    public static QuestionDto toDto(Question question) {

        // -------------------------------
        // Compute MAX points per question
        // -------------------------------
        BigDecimal maxPoints;

        if (question.getScoringMode() == ScoringMode.WEIGHTED) {
            int maxOptionScore = question.getOptions()
                    .stream()
                    .mapToInt(QuestionOption::getScore)
                    .max()
                    .orElse(0);

            maxPoints = BigDecimal.valueOf(maxOptionScore);
        } else {
            // BINARY / legacy
            maxPoints = question.getPoints() == null
                    ? BigDecimal.ZERO
                    : question.getPoints();
        }

        // -------------------------------
        // Map options (NO scores exposed)
        // -------------------------------
        List<OptionDto> optionDtos = question.getOptions()
                .stream()
                .sorted(Comparator.comparingInt(QuestionOption::getOptionNo))
                .map(QuestionMapper::toOptionDto)
                .toList();

        return new QuestionDto(
                question.getId(),
                question.getQuestionNo(),
                question.getType().name(),
                question.getText(),
                maxPoints.toPlainString(),   // ✅ FIXED
                question.isRequired(),
                optionDtos
        );
    }

    private static OptionDto toOptionDto(QuestionOption option) {
        return new OptionDto(
                option.getId(),
                option.getOptionNo(),
                option.getText(),
                option.getScore()
        );
    }
}
