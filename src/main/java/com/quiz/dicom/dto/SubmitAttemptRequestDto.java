package com.quiz.dicom.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record SubmitAttemptRequestDto(
        OffsetDateTime submittedAt,
        Map<Long, List<Long>> answers  // questionId -> selected optionIds
) {}
