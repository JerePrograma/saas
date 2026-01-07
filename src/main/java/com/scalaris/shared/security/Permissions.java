// ============================================================================
// shared/security/Permissions.java
// ============================================================================
// Canonical format: lower + dots. Example: identity.users.manage
package com.scalaris.shared.security;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Permissions {
    private Permissions() {}

    // Canonical format: lower + dots
    private static final Pattern CANONICAL = Pattern.compile("^[a-z0-9]+(\\.[a-z0-9]+)*$");

    // ===== Platform =====
    public static final String PLATFORM_TENANTS_MANAGE = "platform.tenants.manage";

    // ===== Identity =====
    public static final String IDENTITY_LOGIN        = "identity.login";
    public static final String IDENTITY_READ         = "identity.read";
    public static final String IDENTITY_USERS_MANAGE = "identity.users.manage";
    public static final String IDENTITY_ROLES_MANAGE = "identity.roless.manage";

    // ---- Backward compatible aliases (si ya lo tenías usado en código) ----
    /** @deprecated Use {@link #PLATFORM_TENANTS_MANAGE} */
    @Deprecated public static final String PLATFORM_TENANT_MANAGE = PLATFORM_TENANTS_MANAGE;
    /** @deprecated Use {@link #IDENTITY_USERS_MANAGE} */
    @Deprecated public static final String IDENTITY_USER_MANAGE = IDENTITY_USERS_MANAGE;
    /** @deprecated Use {@link #IDENTITY_ROLES_MANAGE} */
    @Deprecated public static final String IDENTITY_ROLE_MANAGE = IDENTITY_ROLES_MANAGE;

    // ---------------------------------------------------------------------
    // Helpers (defensivo): canonicalizar + validar formato
    // ---------------------------------------------------------------------
    public static String canonicalize(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        return s.toLowerCase(Locale.ROOT);
    }

    public static boolean isCanonical(String code) {
        return code != null && !code.isBlank() && CANONICAL.matcher(code).matches();
    }

    public static void assertCanonical(String code) {
        if (!isCanonical(code)) {
            throw new IllegalArgumentException("Permiso inválido (formato requerido: lower.dot): " + code);
        }
    }

    /**
     * Canonicaliza a lower y valida formato.
     * No valida “catálogo”, solo formato (a propósito): permite módulos futuros.
     */
    public static Set<String> canonicalizeAndValidate(Set<String> codes) {
        if (codes == null || codes.isEmpty()) return Set.of();

        return codes.stream()
                .map(Permissions::canonicalize)
                .filter(Objects::nonNull)
                .peek(Permissions::assertCanonical)
                .collect(Collectors.toUnmodifiableSet());
    }
}
