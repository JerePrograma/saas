package com.scalaris.shared.tenancy;

import com.scalaris.bootstrap.security.SessionAuthFilter.IdentityPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class TenantContextFilter extends OncePerRequestFilter {

    private final TenantResolver tenantResolver;

    // Endpoints que NO requieren tenant context (si querés permitirlos sin headers)
    // Si preferís requerir X-Tenant-Key también acá, dejá esto vacío y no bypassees.
    private static final Set<String> PUBLIC_PATH_PREFIXES = Set.of(
            "/api/v1/auth/login"
            // opcional:
            // "/api/v1/auth/password-reset/request",
            // "/api/v1/auth/password-reset/confirm"
    );

    public TenantContextFilter(TenantResolver tenantResolver) {
        this.tenantResolver = tenantResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Bypass fino: solo para paths exactos/prefix. Ajustá a tu gusto.
        return PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        try {
            UUID tenantId = resolveTenantId(req);

            if (tenantId == null) {
                writeBadRequest(res, "TENANT_REQUIRED",
                        "Debe enviar X-Tenant-Id o X-Tenant-Key (o autenticarse)");
                return;
            }

            TenantContext.set(tenantId);
            chain.doFilter(req, res);

        } finally {
            TenantContext.clear();
        }
    }

    private UUID resolveTenantId(HttpServletRequest req) {
        // 1) Post-auth: derivar desde principal
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof IdentityPrincipal p) {
            return p.tenantId();
        }

        // 2) Pre-auth: header id o key
        UUID tenantIdHeader = parseUuidHeader(req.getHeader(TenantResolver.HEADER_TENANT_ID));
        String tenantKeyHeader = req.getHeader(TenantResolver.HEADER_TENANT_KEY);

        // TenantResolver ya valida presencia de alguno si corresponde
        if (tenantIdHeader != null) return tenantIdHeader;
        if (tenantKeyHeader != null && !tenantKeyHeader.isBlank()) {
            return tenantResolver.requireTenantId(null, tenantKeyHeader);
        }

        return null;
    }

    private UUID parseUuidHeader(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void writeBadRequest(HttpServletResponse res, String code, String message) throws IOException {
        res.setStatus(400);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
