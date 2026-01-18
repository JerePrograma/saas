package com.scalaris.config;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.scalaris.api.ApiError;
import com.scalaris.api.ErrorCodes;
import com.scalaris.api.FieldViolation;
import com.scalaris.auth.service.EmailAlreadyRegisteredException;
import com.scalaris.auth.service.InvalidCredentialsException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    // ---------------------------
    // 409 - negocio puntual
    // ---------------------------
    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiError> emailAlreadyRegistered(EmailAlreadyRegisteredException ex) {
        String msg = safe(ex.getMessage(), "Ya te encuentras registrado");
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(ErrorCodes.EMAIL_ALREADY_REGISTERED, msg));
    }

    // ---------------------------
    // 401 - auth
    // ---------------------------
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> invalidCredentials(InvalidCredentialsException ex) {
        // Mensaje fijo (no filtra info sensible)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(ErrorCodes.INVALID_CREDENTIALS, "Correo o contraseña inválidos. Intenta nuevamente."));
    }

    @ExceptionHandler(JWTVerificationException.class)
    public ResponseEntity<ApiError> invalidToken(JWTVerificationException ex) {
        String msg = safe(ex.getMessage(), "Token inválido");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(ErrorCodes.INVALID_TOKEN, msg));
    }

    // Spring Security (por si te cae antes de tu lógica)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> auth(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(ErrorCodes.UNAUTHORIZED, "No autenticado"));
    }

    // ---------------------------
    // 403
    // ---------------------------
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> forbidden(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(ErrorCodes.FORBIDDEN, "No tienes permisos para realizar esta acción"));
    }

    // ---------------------------
    // 400 - validación / request inválido
    // ---------------------------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        List<FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(ApiExceptionHandler::toViolation)
                .toList();

        return ResponseEntity.badRequest()
                .body(ApiError.of(ErrorCodes.VALIDATION_ERROR, "Hay campos inválidos", violations));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> constraintViolation(ConstraintViolationException ex) {
        List<FieldViolation> violations = ex.getConstraintViolations().stream()
                .map(v -> new FieldViolation(String.valueOf(v.getPropertyPath()), v.getMessage()))
                .toList();

        return ResponseEntity.badRequest()
                .body(ApiError.of(ErrorCodes.VALIDATION_ERROR, "Hay campos inválidos", violations));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException ex) {
        String msg = "Parámetro inválido: " + ex.getName();
        return ResponseEntity.badRequest()
                .body(ApiError.of(ErrorCodes.INVALID_REQUEST, msg));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> malformedJson(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(ErrorCodes.MALFORMED_JSON, "JSON inválido o malformado"));
    }

    // Reglas de negocio (hoy tirás IllegalArgumentException desde service)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> illegalArgument(IllegalArgumentException ex) {
        String msg = safe(ex.getMessage(), "Request inválido");
        return ResponseEntity.badRequest()
                .body(ApiError.of(ErrorCodes.BUSINESS_RULE, msg));
    }

    // ---------------------------
    // 405
    // ---------------------------
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> methodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiError.of(ErrorCodes.METHOD_NOT_ALLOWED, "Método no permitido"));
    }

    // ---------------------------
    // 500 - fallback
    // ---------------------------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> generic(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(ErrorCodes.INTERNAL_ERROR, "Error inesperado"));
    }

    private static FieldViolation toViolation(FieldError fe) {
        return new FieldViolation(fe.getField(), safe(fe.getDefaultMessage(), "Inválido"));
    }

    private static String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    @ExceptionHandler(com.scalaris.shared.errors.NotFoundException.class)
    public ResponseEntity<ApiError> notFound(com.scalaris.shared.errors.NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(com.scalaris.shared.errors.ConflictException.class)
    public ResponseEntity<ApiError> conflict(com.scalaris.shared.errors.ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of("CONFLICT", ex.getMessage()));
    }

}
