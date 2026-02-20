package com.quiz.dicom.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Quiz Controller", description = "Endpoints for browsing quizzes and managing attempts")
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

    @Operation(summary = "List all active quizzes", description = "Fetches a summary of quizzes that are currently available for users.")
    @GetMapping
    public List<QuizSummaryDto> listActive() {
        return quizQueryService.listActiveQuizzes();
    }

    @Operation(summary = "list of quiz questions", description = "fetches list of questions for quiz")
    @GetMapping("/{quizId}/questions")
    public List<QuestionDto> questions(@PathVariable Long quizId) {
        return quizQueryService.getQuizQuestions(quizId);
    }

    @Operation(summary = "Start a quiz attempt", description = "Creates a new attempt record for the logged-in user.")
    @PostMapping("/{quizId}/attempts/start")
    public StartAttemptResponseDto startAttempt(@PathVariable Long quizId) {
        Long userId = currentUserService.currentUserId();
        return attemptService.startAttempt(quizId, userId);
    }

    @Operation(summary = "Display Leaderboard", description = "Display Leaderboard")
    @GetMapping("/{quizId}/leaderboard")
    public LeaderboardResponseDto leaderboard(
            @PathVariable Long quizId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return attemptService.leaderboard(quizId, limit);
    }
}
