package com.quiz.dicom.mapper;

import com.quiz.dicom.dto.SubmitAttemptRequestDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SubmitMapper {

    private SubmitMapper() {}

    public record Selection(Long questionId, Long optionId) {}

    public static List<Selection> flattenSelections(SubmitAttemptRequestDto dto) {
        List<Selection> out = new ArrayList<>();
        if (dto == null || dto.answers() == null) return out;

        for (Map.Entry<Long, List<Long>> e : dto.answers().entrySet()) {
            Long qId = e.getKey();
            List<Long> optionIds = e.getValue();
            if (qId == null || optionIds == null) continue;

            for (Long optId : optionIds) {
                if (optId != null) out.add(new Selection(qId, optId));
            }
        }
        return out;
    }
}
