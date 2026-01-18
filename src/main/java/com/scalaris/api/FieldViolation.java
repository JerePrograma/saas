package com.scalaris.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FieldViolation", description = "Detalle de validación por campo")
public record FieldViolation(
        @Schema(example = "email") String field,
        @Schema(example = "must be a well-formed email address") String message
) {}
