package com.scalaris.shared.errors;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "BusinessRuleException",
        description = "Excepción por regla de negocio violada (se mapea típicamente a 422)."
)
public class BusinessRuleException extends RuntimeException {
    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code es obligatorio");
        this.code = code.trim();
    }

    public BusinessRuleException(String message) {
        this("BUSINESS_RULE", message);
    }

    @Schema(description = "Código estable de la regla de negocio.", example = "BUSINESS_RULE")
    public String getCode() { return code; }
}
