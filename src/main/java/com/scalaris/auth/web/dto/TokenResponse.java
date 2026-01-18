package com.scalaris.auth.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TokenResponse", description = "Respuesta de emisión de tokens")
public record TokenResponse(
        @Schema(example = "Bearer") String tokenType,
        @Schema(example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...") String accessToken,
        @Schema(example = "900") long accessExpiresInSeconds,
        @Schema(example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...") String refreshToken,
        @Schema(example = "604800") long refreshExpiresInSeconds
) {}
