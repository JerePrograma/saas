package com.scalaris.shared.errors;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "ForbiddenException",
        description = "Excepción por falta de permisos (se mapea típicamente a 403)."
)
public class ForbiddenException extends RuntimeException {
    private final String code;

    public ForbiddenException(String code, String message) {
        super(message);
        this.code = (code == null || code.isBlank()) ? "FORBIDDEN" : code.trim();
    }

    @Schema(description = "Código estable de autorización.", example = "FORBIDDEN")
    public String getCode() { return code; }
}
