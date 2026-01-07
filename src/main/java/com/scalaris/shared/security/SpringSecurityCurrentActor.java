// ============================================================================
// shared/security/SpringSecurityCurrentActor.java
// ============================================================================
package com.scalaris.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SpringSecurityCurrentActor implements CurrentActor {

    @Override
    public UUID userId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || a.getPrincipal() == null) return null;

        // Opción simple: principal = String UUID
        if (a.getPrincipal() instanceof String s) {
            try { return UUID.fromString(s); } catch (Exception ignored) {}
        }

        return null; // si no hay, queda null (sistema) - si querés, lo hacés required
    }

    @Override
    public boolean has(String permission) {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null) return false;
        for (GrantedAuthority ga : a.getAuthorities()) {
            if (permission.equals(ga.getAuthority())) return true;
        }
        return false;
    }
}
