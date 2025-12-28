package com.quiz.dicom.controller;

import com.quiz.dicom.dto.AttemptResultDto;
import com.quiz.dicom.dto.AttemptReviewDto;
import com.quiz.dicom.dto.SubmitAttemptRequestDto;
import com.quiz.dicom.service.AttemptService;
import com.quiz.dicom.service.CurrentUserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attempts")
public class AttemptController {

    private final AttemptService attemptService;
    private final CurrentUserService currentUserService;

    public AttemptController(AttemptService attemptService, CurrentUserService currentUserService) {
        this.attemptService = attemptService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/{attemptId}/submit")
    public AttemptResultDto submit(
            @PathVariable Long attemptId,
            @RequestBody SubmitAttemptRequestDto req
    ) {
        Long userId = currentUserService.currentUserId();
        return attemptService.submitAttempt(attemptId, userId, req);
    }
    // NEW: Review endpoint
    @GetMapping("/{attemptId}/review")
    public AttemptReviewDto review(@PathVariable Long attemptId) {
        Long userId = currentUserService.currentUserId();
        return attemptService.getAttemptReview(attemptId, userId);
    }
}
