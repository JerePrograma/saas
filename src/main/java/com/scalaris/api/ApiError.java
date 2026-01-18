package com.scalaris.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(name = "ApiError", description = "Error estándar de la API")
public record ApiError(
        @Schema(example = "VALIDATION_ERROR") String code,
        @Schema(example = "Hay campos inválidos") String message,
        @Schema(example = "2026-01-13T16:12:30.309-03:00") OffsetDateTime timestamp,
        List<FieldViolation> violations
) {
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, OffsetDateTime.now(), List.of());
    }

    public static ApiError of(String code, String message, List<FieldViolation> violations) {
        return new ApiError(code, message, OffsetDateTime.now(), violations == null ? List.of() : violations);
    }
}
