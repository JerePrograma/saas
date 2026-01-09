package com.scalaris.shared.errors;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "ConflictException",
        description = "Excepción por conflicto (se mapea típicamente a 409)."
)
public class ConflictException extends RuntimeException {
    private final String code;

    public ConflictException(String code, String message) {
        super(message);
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code es obligatorio");
        this.code = code.trim();
    }

    @Schema(description = "Código estable del conflicto.", example = "CONFLICT")
    public String getCode() { return code; }
}
