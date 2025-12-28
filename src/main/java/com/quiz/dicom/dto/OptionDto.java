// com.quiz.dicom.dto.OptionDto.java
package com.quiz.dicom.dto;

public record OptionDto(
        Long id,
        Integer optionNo,
        String text,
        Integer score // <-- ADD THIS
) {}
