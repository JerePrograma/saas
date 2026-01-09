package com.scalaris.shared.errors;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(
        name = "ApiError",
        description = """
                Respuesta estándar de error de la API.
                
                - `code`: código estable para manejo programático.
                - `message`: mensaje apto para UI/log.
                - `fieldErrors`: solo para validaciones.
                - `traceId`: correlación para logs/tracing.
                """
)
public record ApiErrorDto(
        @Schema(description = "Timestamp ISO-8601 del error.", example = "2026-01-09T16:20:30.123-03:00")
        OffsetDateTime timestamp,

        @Schema(description = "HTTP status numérico.", example = "400")
        int status,

        @Schema(description = "Código de error estable.", example = "VALIDATION_ERROR")
        String code,

        @Schema(description = "Mensaje humano del error.", example = "Hay errores de validación.")
        String message,

        @ArraySchema(
                schema = @Schema(implementation = FieldErrorDto.class),
                arraySchema = @Schema(description = "Errores por campo (si aplica).")
        )
        List<FieldErrorDto> fieldErrors,

        @Schema(description = "ID de trace/correlación.", example = "c7b1e7b0f6d24c6f9b6b9a7c6e2a9f43")
        String traceId,

        @Schema(description = "Path del request que falló.", example = "/api/v1/identity/users")
        String path
) {
    public static ApiErrorDto of(
            int status, String code, String message,
            List<FieldErrorDto> fieldErrors,
            String traceId, String path
    ) {
        return new ApiErrorDto(
                OffsetDateTime.now(),
                status,
                code,
                message,
                fieldErrors == null ? List.of() : List.copyOf(fieldErrors),
                traceId,
                path
        );
    }

    // Ejemplos “reusables” (útiles para @ApiResponse examples)
    public static ApiErrorDto exampleValidation() {
        return new ApiErrorDto(
                OffsetDateTime.parse("2026-01-09T16:20:30.123-03:00"),
                400,
                "VALIDATION_ERROR",
                "Hay errores de validación.",
                List.of(new FieldErrorDto("email", "must be a well-formed email address")),
                "c7b1e7b0f6d24c6f9b6b9a7c6e2a9f43",
                "/api/v1/auth/login"
        );
    }

    public static ApiErrorDto exampleBusinessRule() {
        return new ApiErrorDto(
                OffsetDateTime.parse("2026-01-09T16:20:30.123-03:00"),
                422,
                "SESSION_TOKEN_REQUIRED",
                "Falta token (Bearer o cookie)",
                List.of(),
                "c7b1e7b0f6d24c6f9b6b9a7c6e2a9f43",
                "/api/v1/auth/logout"
        );
    }
}
