package com.scalaris.auth.web.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PasswordResetConfirmRequest(
        @NotNull UUID resetId,
        @NotBlank String code,
        @NotBlank String newPassword,
        @NotBlank String confirmPassword
) {}
