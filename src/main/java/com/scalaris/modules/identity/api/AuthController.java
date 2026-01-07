// ============================================================================
// modules/identity/api/AuthController.java
// ============================================================================

package com.scalaris.modules.identity.api;

import com.scalaris.bootstrap.security.SessionAuthFilter.IdentityPrincipal;
import com.scalaris.modules.identity.api.dto.AuthDtos;
import com.scalaris.modules.identity.application.AuthAppService;
import com.scalaris.shared.errors.BusinessRuleException;
import com.scalaris.shared.tenancy.TenantResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Auth", description = "Login/logout y perfil. Token opaco en Authorization: Bearer o cookie HttpOnly.")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String COOKIE_NAME = "SESSION";

    private final AuthAppService auth;
    private final TenantResolver tenantResolver;

    public AuthController(AuthAppService auth, TenantResolver tenantResolver) {
        this.auth = auth;
        this.tenantResolver = tenantResolver;
    }

    // -------------------------------------------------------------------------
    // PRE-AUTH: requiere tenant context (por tenant key / slug)
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Login",
            description = "Requiere X-Tenant-Key (slug, ej: 'demo'). Devuelve token opaco y setea cookie HttpOnly 'SESSION'."
    )
    @PostMapping("/login")
    public AuthDtos.LoginResponse login(
            @RequestHeader(value = TenantResolver.HEADER_TENANT_KEY) String tenantKeyHeader,
            @Valid @RequestBody AuthDtos.LoginRequest req,
            @RequestHeader(value = "X-Forwarded-For", required = false) String xff,
            @RequestHeader(value = "User-Agent", required = false) String ua,
            HttpServletResponse res
    ) {
        // NOTA: X-Tenant-Key debe ser SLUG (tenant_key.key_value), no UUID.
        UUID tenantId = tenantResolver.requireTenantId(null, tenantKeyHeader);

        String ip = extractClientIp(xff);
        AuthDtos.LoginResponse out = auth.login(tenantId, req, ip, ua);

        Cookie c = new Cookie(COOKIE_NAME, out.sessionToken());
        c.setHttpOnly(true);
        c.setPath("/");
        c.setMaxAge(Boolean.TRUE.equals(req.rememberMe()) ? 60 * 60 * 24 * 30 : 60 * 60 * 8);
        // Recomendado si ya estás en HTTPS:
        // c.setSecure(true);
        // Si querés aislar CSRF por cookie:
        // c.setSameSite("Lax"); // (ojo: en Servlet Cookie no siempre está directo; depende versión)
        res.addCookie(c);

        return out;
    }

    @Operation(
            summary = "Solicitar reset de contraseña",
            description = "Requiere X-Tenant-Key (slug, ej: 'demo')."
    )
    @PostMapping("/password-reset/request")
    public void requestReset(
            @RequestHeader(value = TenantResolver.HEADER_TENANT_KEY) String tenantKeyHeader,
            @Valid @RequestBody AuthDtos.PasswordResetRequest req
    ) {
        UUID tenantId = tenantResolver.requireTenantId(null, tenantKeyHeader);
        auth.requestPasswordReset(tenantId, req);
    }

    @Operation(
            summary = "Confirmar reset de contraseña",
            description = "Versión actual: requiere X-Tenant-Key. Recomendación futura: derivar tenant desde el token y eliminar header."
    )
    @PostMapping("/password-reset/confirm")
    public void confirmReset(
            @RequestHeader(value = TenantResolver.HEADER_TENANT_KEY) String tenantKeyHeader,
            @Valid @RequestBody AuthDtos.PasswordResetConfirm req
    ) {
        UUID tenantId = tenantResolver.requireTenantId(null, tenantKeyHeader);
        auth.confirmPasswordReset(tenantId, req);
    }

    // -------------------------------------------------------------------------
    // POST-AUTH: tenant se deriva del principal (NO headers de tenant)
    // -------------------------------------------------------------------------

    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Logout",
            description = "Usa el tenant del token/sesión. No acepta X-Tenant-Key/X-Tenant-Id."
    )
    @PostMapping("/logout")
    public void logout(
            Authentication authentication,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @CookieValue(value = COOKIE_NAME, required = false) String cookieToken,
            HttpServletResponse res
    ) {
        IdentityPrincipal p = (IdentityPrincipal) authentication.getPrincipal();

        String token = extractBearer(authHeader);
        if (token == null) token = cookieToken;

        if (token == null || token.isBlank()) {
            throw new BusinessRuleException("SESSION_TOKEN_REQUIRED", "Falta token (Bearer o cookie)");
        }

        auth.logout(p.tenantId(), p.userId(), token);

        // best-effort: borrar cookie cliente
        Cookie c = new Cookie(COOKIE_NAME, "");
        c.setHttpOnly(true);
        c.setPath("/");
        c.setMaxAge(0);
        // c.setSecure(true);
        res.addCookie(c);
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Perfil actual",
            description = "Usa el tenant del token/sesión. No acepta X-Tenant-Key/X-Tenant-Id."
    )
    @GetMapping("/me")
    public AuthDtos.MeResponse me(Authentication authentication) {
        IdentityPrincipal p = (IdentityPrincipal) authentication.getPrincipal();
        return auth.me(p.tenantId(), p.userId());
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private String extractClientIp(String xff) {
        if (xff == null || xff.isBlank()) return null;
        // "client, proxy1, proxy2"
        String[] parts = xff.split(",");
        return (parts.length == 0) ? null : parts[0].trim();
    }

    private String extractBearer(String header) {
        if (header == null) return null;
        if (!header.startsWith("Bearer ")) return null;
        String token = header.substring(7).trim();
        return token.isBlank() ? null : token;
    }
}
