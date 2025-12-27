package com.quiz.dicom.mapper;

import com.quiz.dicom.domain.Question;
import com.quiz.dicom.domain.QuestionOption;
import com.quiz.dicom.dto.OptionDto;
import com.quiz.dicom.dto.QuestionDto;

import java.util.Comparator;
import java.util.List;

public final class QuestionMapper {

    private QuestionMapper() {}

    public static QuestionDto toDto(Question question) {
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
                question.getPoints() == null ? "0" : question.getPoints().toPlainString(),
                question.isRequired(),
                optionDtos
        );
    }

    private static OptionDto toOptionDto(QuestionOption option) {
        return new OptionDto(
                option.getId(),
                option.getOptionNo(),
                option.getText()
        );
    }
}
