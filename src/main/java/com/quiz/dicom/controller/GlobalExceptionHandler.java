package com.quiz.dicom.controller;

import com.quiz.dicom.dto.ApiErrorDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorDto> handleBadRequest(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex, req);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorDto> handleConflict(IllegalStateException ex, HttpServletRequest req) {
        // Using 409 for state-related conflicts (already submitted, etc.)
        return build(HttpStatus.CONFLICT, ex, req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorDto> handleForbidden(AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, ex, req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDto> handleGeneric(Exception ex, HttpServletRequest req) {
        // Avoid leaking internal details in production; keep message generic if you prefer.
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex, req);
    }

    private ResponseEntity<ApiErrorDto> build(HttpStatus status, Exception ex, HttpServletRequest req) {
        ApiErrorDto body = new ApiErrorDto(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                req.getRequestURI(),
                List.of()
        );
        return ResponseEntity.status(status).body(body);
    }
}
