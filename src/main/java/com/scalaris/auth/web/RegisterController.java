package com.scalaris.auth.web;

import com.scalaris.auth.service.EmailAlreadyRegisteredException;
import com.scalaris.auth.service.RegistrationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
public class RegisterController {

    private final RegistrationService registration;

    public RegisterController(RegistrationService registration) {
        this.registration = registration;
    }

    // Step1 helper: para que React valide email único antes de pasar al Step2
    @PostMapping("/register/check-email")
    public ResponseEntity<Void> checkEmail(@RequestBody @Valid CheckEmailRequest req) {
        boolean available = registration.isEmailAvailable(req.email());
        return available ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    // Registro final: Step1 + Step2
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody @Valid com.scalaris.auth.web.dto.RegisterRequest req) {
        var user = registration.register(req);

        var body = new RegisterResponse(
                user.getId().toString(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                user.getCreatedAt().toString()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // ===== DTOs de respuesta / request puntuales del controller (siguen siendo records) =====

    public record CheckEmailRequest(@NotBlank @Email String email) {}

    public record RegisterResponse(
            String id,
            String fullName,
            String email,
            String role,
            String createdAt
    ) {}

    public record ApiError(
            String code,
            String message,
            OffsetDateTime timestamp,
            List<FieldViolation> violations
    ) {}

    public record FieldViolation(String field, String message) {}

    // ===== Exception mapping (parte de la capa web/MVC) =====

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiError> emailAlreadyRegistered(EmailAlreadyRegisteredException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiError(
                        "EMAIL_ALREADY_REGISTERED",
                        ex.getMessage(),
                        OffsetDateTime.now(),
                        List.of()
                )
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> illegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(
                new ApiError(
                        "INVALID_REGISTRATION",
                        ex.getMessage(),
                        OffsetDateTime.now(),
                        List.of()
                )
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        List<FieldViolation> violations = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new FieldViolation(fieldPath(fe), fe.getDefaultMessage()))
                .toList();

        return ResponseEntity.badRequest().body(
                new ApiError(
                        "VALIDATION_ERROR",
                        "Hay campos inválidos",
                        OffsetDateTime.now(),
                        violations
                )
        );
    }

    private static String fieldPath(FieldError fe) {
        // Ej: "step1.email" / "step2.acceptedTerms"
        return fe.getField();
    }
}
