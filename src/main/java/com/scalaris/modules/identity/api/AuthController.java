package com.scalaris.modules.identity.api;

import com.scalaris.bootstrap.security.SessionAuthFilter.IdentityPrincipal;
import com.scalaris.modules.identity.api.dto.AuthDtos;
import com.scalaris.modules.identity.application.AuthAppService;
import com.scalaris.shared.errors.BusinessRuleException;
import com.scalaris.shared.tenancy.TenantResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "01 - Auth (Public)")
public class AuthController {

    private final AuthAppService auth;
    private final TenantResolver tenantResolver;
    private final SessionCookieSupport cookies;

    public AuthController(AuthAppService auth, TenantResolver tenantResolver, SessionCookieSupport cookies) {
        this.auth = auth;
        this.tenantResolver = tenantResolver;
        this.cookies = cookies;
    }

    @Operation(summary = "Login")
    @SecurityRequirement(name = "tenantKey")
    @PostMapping("/login")
    public AuthDtos.LoginResponse login(
            @RequestHeader(TenantResolver.HEADER_TENANT_KEY) String tenantKey,
            @Valid @RequestBody AuthDtos.LoginRequest req,
            @Parameter(hidden = true) HttpServletRequest httpReq,
            @Parameter(hidden = true) HttpServletResponse res
    ) {
        UUID tenantId = tenantResolver.requireTenantId(null, tenantKey);

        String ip = RequestContextSupport.clientIp(httpReq);
        String ua = httpReq.getHeader("User-Agent");

        AuthDtos.LoginResponse out = auth.login(tenantId, req, ip, ua);

        cookies.setSession(res, out.sessionToken(), Boolean.TRUE.equals(req.rememberMe()));
        return out;
    }

    @Operation(summary = "Solicitar reset password (respuesta siempre OK)")
    @SecurityRequirement(name = "tenantKey")
    @PostMapping("/password-reset/request")
    public void requestReset(
            @RequestHeader(TenantResolver.HEADER_TENANT_KEY) String tenantKey,
            @Valid @RequestBody AuthDtos.PasswordResetRequest req
    ) {
        UUID tenantId = tenantResolver.requireTenantId(null, tenantKey);
        auth.requestPasswordReset(tenantId, req);
    }

    @Operation(summary = "Confirmar reset password")
    @SecurityRequirement(name = "tenantKey")
    @PostMapping("/password-reset/confirm")
    public void confirmReset(
            @RequestHeader(TenantResolver.HEADER_TENANT_KEY) String tenantKey,
            @Valid @RequestBody AuthDtos.PasswordResetConfirm req
    ) {
        UUID tenantId = tenantResolver.requireTenantId(null, tenantKey);
        auth.confirmPasswordReset(tenantId, req);
    }

    @Operation(summary = "Logout")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public void logout(
            @Parameter(hidden = true) @AuthenticationPrincipal IdentityPrincipal p,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @CookieValue(value = SessionCookieSupport.COOKIE_NAME, required = false) String cookieToken,
            @Parameter(hidden = true) HttpServletResponse res
    ) {
        String token = TokenSupport.resolve(authHeader, cookieToken);
        if (token == null) throw new BusinessRuleException("SESSION_TOKEN_REQUIRED", "Falta token (Bearer o cookie)");

        auth.logout(p.tenantId(), p.userId(), token);
        cookies.clearSession(res);
    }

    @Operation(summary = "Perfil actual")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public AuthDtos.MeResponse me(
            @Parameter(hidden = true) @AuthenticationPrincipal IdentityPrincipal p
    ) {
        return auth.me(p.tenantId(), p.userId());
    }
}
