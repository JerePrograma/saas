package com.scalaris.bootstrap.api;

import com.scalaris.shared.errors.BusinessRuleException;
import com.scalaris.shared.errors.NotFoundException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<?> notFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body("NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<?> business(BusinessRuleException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body("BUSINESS_RULE", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> generic() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body("ERROR", "Error inesperado"));
    }

    private Map<String, Object> body(String code, String message) {
        return Map.of(
                "timestamp", OffsetDateTime.now().toString(),
                "code", code,
                "message", message
        );
    }
}
