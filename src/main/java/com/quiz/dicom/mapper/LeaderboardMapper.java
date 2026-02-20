package com.quiz.dicom.mapper;

import com.quiz.dicom.domain.Attempt;
import com.quiz.dicom.dto.LeaderboardEntryDto;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class LeaderboardMapper {

    private LeaderboardMapper() {}

    public static List<LeaderboardEntryDto> toLeaderboard(List<Attempt> attempts) {
        AtomicLong rank = new AtomicLong(1);

        return attempts.stream()
                .map(a -> new LeaderboardEntryDto(
                        rank.getAndIncrement(),
                        a.getUser() == null ? null : a.getUser().getId(),
                        a.getUser() == null ? "Anonymous" : safeName(a.getUser().getFullName(), a.getUser().getEmail()),
                        a.getScore() == null ? "0" : a.getScore().toPlainString(),
                        a.getSubmittedAt()
                ))
                .toList();
    }

    private static String safeName(String fullName, String email) {
        if (fullName != null && !fullName.isBlank()) return fullName;
        if (email != null && !email.isBlank()) return email;
        return "User";
    }
}
