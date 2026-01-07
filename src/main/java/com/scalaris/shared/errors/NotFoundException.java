// ============================================================================
// shared/errors/NotFoundException.java
// ============================================================================
package com.scalaris.shared.errors;

public class NotFoundException extends RuntimeException {
    private final String code;

    public NotFoundException(String code, String message) {
        super(message);
        this.code = (code == null || code.isBlank()) ? "NOT_FOUND" : code.trim();
    }

    // Overload para usos comunes
    public NotFoundException(String message) {
        this("NOT_FOUND", message);
    }

    public String getCode() { return code; }
}
