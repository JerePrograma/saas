// ============================================================================
// shared/errors/ForbiddenException.java
// ============================================================================
package com.scalaris.shared.errors;

public class ForbiddenException extends RuntimeException {
    private final String code;

    public ForbiddenException(String code, String message) {
        super(message);
        this.code = (code == null || code.isBlank()) ? "FORBIDDEN" : code.trim();
    }

    public String getCode() { return code; }
}
