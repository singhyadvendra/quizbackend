package com.quiz.dicom.controller;

import com.quiz.dicom.dto.LeaderboardResponseDto;
import com.quiz.dicom.dto.QuestionDto;
import com.quiz.dicom.dto.QuizSummaryDto;
import com.quiz.dicom.dto.StartAttemptResponseDto;
import com.quiz.dicom.service.AttemptService;
import com.quiz.dicom.service.CurrentUserService;
import com.quiz.dicom.service.QuizQueryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    private final QuizQueryService quizQueryService;
    private final AttemptService attemptService;
    private final CurrentUserService currentUserService;

    public QuizController(QuizQueryService quizQueryService,
                          AttemptService attemptService,
                          CurrentUserService currentUserService) {
        this.quizQueryService = quizQueryService;
        this.attemptService = attemptService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<QuizSummaryDto> listActive() {
        return quizQueryService.listActiveQuizzes();
    }

    @GetMapping("/{quizId}/questions")
    public List<QuestionDto> questions(@PathVariable Long quizId) {
        return quizQueryService.getQuizQuestions(quizId);
    }

    @PostMapping("/{quizId}/attempts/start")
    public StartAttemptResponseDto startAttempt(@PathVariable Long quizId) {
        Long userId = currentUserService.currentUserId();
        return attemptService.startAttempt(quizId, userId);
    }

    @GetMapping("/{quizId}/leaderboard")
    public LeaderboardResponseDto leaderboard(
            @PathVariable Long quizId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return attemptService.leaderboard(quizId, limit);
    }
}
