package com.scalaris.modules.identity.api.dto;

import jakarta.validation.constraints.*;
import java.util.Set;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos(){}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password,
            Boolean rememberMe
    ) {}

    public record LoginResponse(
            UUID tenantId,
            UUID userId,
            String fullName,
            String email,
            String sessionToken,
            Set<String> roles,
            Set<String> permissions
    ) {}

    public record MeResponse(
            UUID tenantId,
            UUID userId,
            String fullName,
            String email,
            String status,
            Set<String> roles,
            Set<String> permissions
    ) {}

    public record PasswordResetRequest(@NotBlank @Email String email) {}

    public record PasswordResetConfirm(
            @NotBlank String token,
            @NotBlank @Size(min = 8, max = 100) String newPassword
    ) {}
}
