package com.scalaris.shared.errors;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "FieldError",
        description = "Detalle de error de validación por campo."
)
public record FieldErrorDto(
        @Schema(description = "Nombre del campo/propiedad con error.", example = "email", nullable = true)
        String field,

        @Schema(description = "Mensaje de validación asociado.", example = "must be a well-formed email address")
        String message
) { }
