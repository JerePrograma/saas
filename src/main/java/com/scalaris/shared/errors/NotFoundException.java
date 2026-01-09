package com.scalaris.shared.errors;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "NotFoundException",
        description = "Excepción de recurso no encontrado (se mapea típicamente a 404)."
)
public class NotFoundException extends RuntimeException {
    private final String code;

    public NotFoundException(String code, String message) {
        super(message);
        this.code = (code == null || code.isBlank()) ? "NOT_FOUND" : code.trim();
    }

    public NotFoundException(String message) {
        this("NOT_FOUND", message);
    }

    @Schema(description = "Código estable de not-found.", example = "NOT_FOUND")
    public String getCode() { return code; }
}
