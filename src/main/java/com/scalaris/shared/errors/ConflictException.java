// ============================================================================
// shared/errors/ConflictException.java
// ============================================================================
package com.scalaris.shared.errors;

public class ConflictException extends RuntimeException {
    private final String code;

    public ConflictException(String code, String message) {
        super(message);
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code es obligatorio");
        this.code = code.trim();
    }

    public String getCode() { return code; }
}
