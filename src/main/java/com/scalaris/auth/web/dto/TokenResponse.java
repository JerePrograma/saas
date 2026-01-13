package com.scalaris.auth.web.dto;

public record TokenResponse(
        String tokenType,
        String accessToken,
        long accessExpiresInSeconds,
        String refreshToken,
        long refreshExpiresInSeconds
) {}
