package com.scalaris.auth.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "LoginRequest", description = "Credenciales de login")
public record LoginRequest(
        @NotBlank @Email
        @Schema(example = "user@demo.com")
        String email,

        @NotBlank
        @Schema(example = "Abcdef12")
        String password
) {}
