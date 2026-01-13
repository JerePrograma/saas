package com.scalaris.config;

import com.scalaris.auth.service.EmailAlreadyRegisteredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    public record ApiError(
            String code,
            String message,
            OffsetDateTime timestamp,
            List<FieldViolation> violations
    ) {}

    public record FieldViolation(String field, String message) {}

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiError> emailAlreadyRegistered(EmailAlreadyRegisteredException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiError("EMAIL_ALREADY_REGISTERED", ex.getMessage(), OffsetDateTime.now(), List.of())
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> illegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(
                new ApiError("INVALID_REQUEST", ex.getMessage(), OffsetDateTime.now(), List.of())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        List<FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(ApiExceptionHandler::toViolation)
                .toList();

        return ResponseEntity.badRequest().body(
                new ApiError("VALIDATION_ERROR", "Hay campos inválidos", OffsetDateTime.now(), violations)
        );
    }

    private static FieldViolation toViolation(FieldError fe) {
        return new FieldViolation(fe.getField(), fe.getDefaultMessage());
    }
}
