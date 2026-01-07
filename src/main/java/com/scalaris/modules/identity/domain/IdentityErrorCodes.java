package com.scalaris.modules.identity.domain;

public final class IdentityErrorCodes {
    private IdentityErrorCodes() {}

    public static final String AUTH_INVALID_CREDENTIALS = "AUTH_INVALID_CREDENTIALS";

    // Usuario / tenant
    public static final String AUTH_USER_BLOCKED        = "AUTH_USER_BLOCKED";   // LOCKED
    public static final String AUTH_USER_INACTIVE       = "AUTH_USER_INACTIVE";  // INACTIVE
    public static final String AUTH_USER_PENDING        = "AUTH_USER_PENDING";   // PENDING (sin password / invitación)
    public static final String AUTH_TENANT_NOT_ACTIVE   = "AUTH_TENANT_NOT_ACTIVE";

    // Sesión
    public static final String AUTH_SESSION_NOT_FOUND   = "AUTH_SESSION_NOT_FOUND";
    public static final String AUTH_SESSION_REVOKED     = "AUTH_SESSION_REVOKED";
    public static final String AUTH_SESSION_EXPIRED     = "AUTH_SESSION_EXPIRED";

    // Reset
    public static final String AUTH_RESET_TOKEN_INVALID = "AUTH_RESET_TOKEN_INVALID";
}
