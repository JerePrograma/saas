package com.scalaris.modules.identity.api;

public final class TokenSupport {
    private TokenSupport() {}

    public static String resolve(String authHeader, String cookieToken) {
        String bearer = extractBearer(authHeader);
        if (bearer != null) return bearer;
        if (cookieToken != null && !cookieToken.isBlank()) return cookieToken.trim();
        return null;
    }

    private static String extractBearer(String header) {
        if (header == null || !header.startsWith("Bearer ")) return null;
        String token = header.substring(7).trim();
        return token.isBlank() ? null : token;
    }
}
