package com.quiz.dicom.controller;

import com.quiz.dicom.dto.admin.*;
import com.quiz.dicom.service.AdminQuizService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminQuizController {

    private final AdminQuizService adminQuizService;

    public AdminQuizController(AdminQuizService adminQuizService) {
        this.adminQuizService = adminQuizService;
    }

    @PostMapping("/quizzes")
    public QuizAdminDto createQuiz(@RequestBody @Valid CreateQuizRequestDto req) {
        return adminQuizService.createQuiz(req);
    }

    @PostMapping("/quizzes/{quizId}/questions")
    public QuestionAdminDto addQuestion(@PathVariable Long quizId,
                                        @RequestBody @Valid CreateQuestionRequestDto req) {
        return adminQuizService.addQuestion(quizId, req);
    }

    @PostMapping("/questions/{questionId}/options")
    public OptionAdminDto addOption(@PathVariable Long questionId,
                                    @RequestBody @Valid CreateOptionRequestDto req) {
        return adminQuizService.addOption(questionId, req);
    }

    @PutMapping("/quizzes/{quizId}/active")
    public QuizAdminDto setActive(@PathVariable Long quizId,
                                  @RequestParam boolean active) {
        return adminQuizService.setQuizActive(quizId, active);
    }
}
