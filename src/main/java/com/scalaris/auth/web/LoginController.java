package com.scalaris.auth.web;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.scalaris.auth.repo.UserRepository;
import com.scalaris.auth.service.AuthService;
import com.scalaris.auth.service.InvalidCredentialsException;
import com.scalaris.auth.service.TokenService;
import com.scalaris.auth.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class LoginController {

    private final AuthService auth;
    private final TokenService tokens;
    private final UserRepository users;

    public LoginController(AuthService auth, TokenService tokens, UserRepository users) {
        this.auth = auth;
        this.tokens = tokens;
        this.users = users;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest req) {
        var user = auth.authenticate(req);
        var issued = tokens.issueTokens(user);

        return ResponseEntity.ok(new TokenResponse(
                "Bearer",
                issued.accessToken(),
                issued.accessExpiresInSeconds(),
                issued.refreshToken(),
                issued.refreshExpiresInSeconds()
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody @Valid RefreshRequest req) {
        var decoded = tokens.verify(req.refreshToken());
        UUID userId = UUID.fromString(decoded.getSubject());

        var user = users.findById(userId)
                .orElseThrow(() -> new JWTVerificationException("Usuario inexistente"));

        var issued = tokens.refresh(req.refreshToken(), user);

        return ResponseEntity.ok(new TokenResponse(
                "Bearer",
                issued.accessToken(),
                issued.accessExpiresInSeconds(),
                issued.refreshToken(),
                issued.refreshExpiresInSeconds()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody @Valid RefreshRequest req) {
        tokens.revokeRefresh(req.refreshToken());
        return ResponseEntity.noContent().build();
    }

    // ---- errores mínimos (no overkill) ----
    public record ApiError(String code, String message, OffsetDateTime timestamp, List<FieldViolation> violations) {}
    public record FieldViolation(String field, String message) {}

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> invalidCreds(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError("INVALID_CREDENTIALS", ex.getMessage(), OffsetDateTime.now(), List.of()));
    }

    @ExceptionHandler(JWTVerificationException.class)
    public ResponseEntity<ApiError> invalidToken(JWTVerificationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError("INVALID_TOKEN", ex.getMessage(), OffsetDateTime.now(), List.of()));
    }
}
