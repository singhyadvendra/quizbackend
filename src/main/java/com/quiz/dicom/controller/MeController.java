package com.quiz.dicom.controller;

import com.quiz.dicom.dto.MeDto;
import com.quiz.dicom.service.CurrentUserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class MeController {

    private final CurrentUserService currentUserService;

    public MeController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping("/me")
    public MeDto me() {
        return currentUserService.me();
    }
}
