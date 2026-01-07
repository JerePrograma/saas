// ============================================================================
// shared/errors/BusinessRuleException.java
// ============================================================================
package com.scalaris.shared.errors;

public class BusinessRuleException extends RuntimeException {
    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code es obligatorio");
        this.code = code.trim();
    }

    // Overload mínimo para no romper call-sites existentes
    public BusinessRuleException(String message) {
        this("BUSINESS_RULE", message);
    }

    public String getCode() { return code; }
}
