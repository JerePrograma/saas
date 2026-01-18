package com.scalaris.auth.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(name = "PasswordResetConfirmRequest", description = "Confirmación de reseteo de contraseña")
public record PasswordResetConfirmRequest(
        @NotNull
        @Schema(example = "c2d8d6f1-5cf8-4ad1-b6e3-4c0cfc2c0f8a")
        UUID resetId,

        @NotBlank
        @Schema(example = "123456")
        String code,

        @NotBlank
        @Schema(example = "Abcdef12")
        String newPassword,

        @NotBlank
        @Schema(example = "Abcdef12")
        String confirmPassword
) {}
