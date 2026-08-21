package com.br.aiassistantlab.common.api;

import com.br.aiassistantlab.chat.application.InvalidChatRequestException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                            HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Request validation failed");
        return ResponseEntity.badRequest().body(error("VALIDATION_FAILED", message, request));
    }

    @ExceptionHandler(InvalidChatRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidChatRequest(InvalidChatRequestException ex,
                                                                    HttpServletRequest request) {
        return ResponseEntity.badRequest().body(error("VALIDATION_FAILED", ex.getMessage(), request));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(HttpMessageNotReadableException ex,
                                                                   HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(error("VALIDATION_FAILED", "Request body is not readable", request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error("INTERNAL_ERROR", "Request failed", request));
    }

    private ApiErrorResponse error(String code, String message, HttpServletRequest request) {
        return new ApiErrorResponse(code, message, request.getRequestURI(), OffsetDateTime.now());
    }
}
