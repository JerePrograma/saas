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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(
        name = "Auth",
        description = """
                Autenticación por **token opaco**.
                
                - **PRE-AUTH**: requiere header `X-Tenant-Key` (slug) para resolver tenant.
                - **POST-AUTH**: tenant se deriva del token/sesión (NO se aceptan headers de tenant).
                
                Transporte de token:
                - `Authorization: Bearer <token>` (recomendado para APIs)
                - Cookie HttpOnly `SESSION` (útil para web)
                """
)
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
    // PRE-AUTH (requiere X-Tenant-Key)
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Login (CUS-01)",
            description = """
                    Autentica un usuario en el **tenant indicado** por `X-Tenant-Key` (slug).
                    
                    Flujo:
                    1) Resuelve tenantId por `X-Tenant-Key` (slug).
                    2) Valida credenciales.
                    3) Crea sesión (token opaco) con expiración (rememberMe extiende duración).
                    4) Devuelve `LoginResponse` y setea cookie HttpOnly `SESSION` (best-effort).
                    
                    Notas:
                    - El token puede usarse por Bearer o por cookie.
                    - Para evitar enumeración, errores pueden colapsar en "Credenciales inválidas".
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login exitoso",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthDtos.LoginResponse.class),
                            examples = @ExampleObject(name = "LoginResponse", value = """
                                    {
                                      "tenantId": "1c8dbd77-0f6d-41d0-9f40-5c3f6ab2c210",
                                      "userId": "b9f2f1b4-0d84-4b89-9f10-1c3f0e4c7b8a",
                                      "fullName": "Owner Admin",
                                      "email": "owner@acme.com",
                                      "sessionToken": "y8yYJ4i9v8E8bJmHcT3o8f7oJm1Jf8wJt8lYbQ",
                                      "roles": ["ADMIN"],
                                      "permissions": [
                                        "identity.login",
                                        "identity.read",
                                        "identity.users.manage",
                                        "identity.roles.manage"
                                      ]
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Credenciales inválidas / regla de negocio",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "AUTH_INVALID_CREDENTIALS", value = """
                                            { "code": "AUTH_INVALID_CREDENTIALS", "message": "Credenciales inválidas" }
                                            """),
                                    @ExampleObject(name = "AUTH_USER_INACTIVE", value = """
                                            { "code": "AUTH_USER_INACTIVE", "message": "Usuario inactivo" }
                                            """),
                                    @ExampleObject(name = "AUTH_USER_BLOCKED", value = """
                                            { "code": "AUTH_USER_BLOCKED", "message": "Usuario bloqueado" }
                                            """),
                                    @ExampleObject(name = "AUTH_EMPTY_PERMISSIONS", value = """
                                            { "code": "AUTH_EMPTY_PERMISSIONS", "message": "El contexto de permisos no puede estar vacío" }
                                            """),
                                    @ExampleObject(name = "AUTH_LOGIN_NOT_ALLOWED", value = """
                                            { "code": "AUTH_LOGIN_NOT_ALLOWED", "message": "Login no permitido" }
                                            """)
                            })),
            @ApiResponse(responseCode = "401", description = "No aplica (endpoint público)", content = @Content),
            @ApiResponse(responseCode = "403", description = "No aplica (endpoint público)", content = @Content)
    })
    @PostMapping("/login")
    public AuthDtos.LoginResponse login(
            @Parameter(
                    name = TenantResolver.HEADER_TENANT_KEY,
                    in = ParameterIn.HEADER,
                    required = true,
                    description = "Slug del tenant (ej: `demo`, `acme`). Se resuelve a tenantId.",
                    example = "acme"
            )
            @RequestHeader(TenantResolver.HEADER_TENANT_KEY) String tenantKey,

            @Valid
            @RequestBody(
                    required = true,
                    description = "Credenciales del usuario",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthDtos.LoginRequest.class),
                            examples = @ExampleObject(value = """
                                    { "email": "owner@acme.com", "password": "Str0ngPass!", "rememberMe": true }
                                    """))
            )
            AuthDtos.LoginRequest req,

            @Parameter(
                    name = "X-Forwarded-For",
                    in = ParameterIn.HEADER,
                    required = false,
                    description = "IP original si estás detrás de proxy/load balancer. Se toma el primer valor.",
                    example = "203.0.113.10, 70.41.3.18"
            )
            @RequestHeader(value = "X-Forwarded-For", required = false) String xff,

            @Parameter(
                    name = "User-Agent",
                    in = ParameterIn.HEADER,
                    required = false,
                    description = "User-Agent del cliente. Se guarda en la sesión (auditoría/seguridad).",
                    example = "Mozilla/5.0 ..."
            )
            @RequestHeader(value = "User-Agent", required = false) String ua,

            HttpServletResponse res
    ) {
        UUID tenantId = tenantResolver.requireTenantId(null, tenantKey);

        String ip = extractClientIp(xff);
        AuthDtos.LoginResponse out = auth.login(tenantId, req, ip, ua);

        // Cookie HttpOnly 'SESSION' (best-effort)
        setSessionCookie(res, out.sessionToken(), Boolean.TRUE.equals(req.rememberMe()));
        return out;
    }

    @Operation(
            summary = "Solicitar reset de contraseña",
            description = """
                    Inicia el flujo de reset de contraseña para un email dentro del tenant.
                    
                    Seguridad:
                    - Respuesta **siempre OK** (no revela si el email existe).
                    - Requiere `X-Tenant-Key` para acotar el tenant (multi-tenant).
                    
                    Implementación típica:
                    - Emite token de reset (opaco) con expiración (ej: 30 min).
                    - Publica evento para enviar email.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud aceptada (siempre OK)", content = @Content),
            @ApiResponse(responseCode = "400", description = "Validación de request (email inválido, etc.)",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "code": "VALIDATION_ERROR", "message": "email: must be a well-formed email address" }
                                    """))),
            @ApiResponse(responseCode = "401", description = "No aplica (endpoint público)", content = @Content),
            @ApiResponse(responseCode = "403", description = "No aplica (endpoint público)", content = @Content)
    })
    @PostMapping("/password-reset/request")
    public void requestReset(
            @Parameter(
                    name = TenantResolver.HEADER_TENANT_KEY,
                    in = ParameterIn.HEADER,
                    required = true,
                    description = "Slug del tenant (ej: `demo`, `acme`).",
                    example = "acme"
            )
            @RequestHeader(TenantResolver.HEADER_TENANT_KEY) String tenantKey,

            @Valid
            @RequestBody(
                    required = true,
                    description = "Email del usuario que solicita reset",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthDtos.PasswordResetRequest.class),
                            examples = @ExampleObject(value = """
                                    { "email": "owner@acme.com" }
                                    """))
            )
            AuthDtos.PasswordResetRequest req
    ) {
        UUID tenantId = tenantResolver.requireTenantId(null, tenantKey);
        auth.requestPasswordReset(tenantId, req);
    }

    @Operation(
            summary = "Confirmar reset de contraseña",
            description = """
                    Confirma el reset usando el token emitido previamente.
                    
                    Reglas típicas:
                    - Token debe ser válido y no expirado.
                    - Marca token como usado (one-time).
                    - Cambia password y revoca sesiones activas (defensa en profundidad).
                    
                    Nota:
                    - Hoy requiere `X-Tenant-Key`. Futuro: derivar tenant desde el token de reset.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reset confirmado", content = @Content),
            @ApiResponse(responseCode = "400", description = "Token inválido/expirado o validación",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "AUTH_RESET_TOKEN_INVALID", value = """
                                            { "code": "AUTH_RESET_TOKEN_INVALID", "message": "Token inválido o expirado" }
                                            """),
                                    @ExampleObject(name = "INVALID_PASSWORD", value = """
                                            { "code": "INVALID_PASSWORD", "message": "Password inválida" }
                                            """)
                            })),
            @ApiResponse(responseCode = "401", description = "No aplica (endpoint público)", content = @Content),
            @ApiResponse(responseCode = "403", description = "No aplica (endpoint público)", content = @Content)
    })
    @PostMapping("/password-reset/confirm")
    public void confirmReset(
            @Parameter(
                    name = TenantResolver.HEADER_TENANT_KEY,
                    in = ParameterIn.HEADER,
                    required = true,
                    description = "Slug del tenant (ej: `demo`, `acme`).",
                    example = "acme"
            )
            @RequestHeader(TenantResolver.HEADER_TENANT_KEY) String tenantKey,

            @Valid
            @RequestBody(
                    required = true,
                    description = "Token de reset + nuevo password",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthDtos.PasswordResetConfirm.class),
                            examples = @ExampleObject(value = """
                                    { "token": "r3s3tT0k3nOpaqueHere", "newPassword": "N3wStr0ngPass!" }
                                    """))
            )
            AuthDtos.PasswordResetConfirm req
    ) {
        UUID tenantId = tenantResolver.requireTenantId(null, tenantKey);
        auth.confirmPasswordReset(tenantId, req);
    }

    // -------------------------------------------------------------------------
    // POST-AUTH (tenant sale del principal)
    // -------------------------------------------------------------------------

    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Logout",
            description = """
                    Cierra sesión (revoca la sesión actual).
                    
                    Token:
                    - Si viene `Authorization: Bearer <token>`, se usa ese.
                    - Si no, se intenta cookie HttpOnly `SESSION`.
                    
                    Efectos:
                    - Revoca sesión en servidor.
                    - Limpia cookie `SESSION` (best-effort).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logout OK (idempotente por sesión revocada)", content = @Content),
            @ApiResponse(responseCode = "400", description = "Falta token",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "code": "SESSION_TOKEN_REQUIRED", "message": "Falta token (Bearer o cookie)" }
                                    """))),
            @ApiResponse(responseCode = "401", description = "No autenticado / token inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Autenticado pero sin permisos (si aplicara)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Sesión no encontrada (si no colapsás a 401)", content = @Content)
    })
    @PostMapping("/logout")
    public void logout(
            Authentication authentication,

            @Parameter(
                    name = HttpHeaders.AUTHORIZATION,
                    in = ParameterIn.HEADER,
                    required = false,
                    description = "Bearer token opaco. Alternativa a cookie.",
                    example = "Bearer y8yYJ4i9v8E8bJmHcT3o8f7oJm1Jf8wJt8lYbQ"
            )
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,

            @Parameter(
                    name = COOKIE_NAME,
                    in = ParameterIn.COOKIE,
                    required = false,
                    description = "Cookie HttpOnly con el token de sesión (fallback si no mandás Bearer).",
                    example = "y8yYJ4i9v8E8bJmHcT3o8f7oJm1Jf8wJt8lYbQ"
            )
            @CookieValue(value = COOKIE_NAME, required = false) String cookieToken,

            HttpServletResponse res
    ) {
        IdentityPrincipal p = (IdentityPrincipal) authentication.getPrincipal();

        String token = resolveToken(authHeader, cookieToken);
        if (token == null) {
            throw new BusinessRuleException("SESSION_TOKEN_REQUIRED", "Falta token (Bearer o cookie)");
        }

        auth.logout(p.tenantId(), p.userId(), token);
        clearSessionCookie(res);
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Perfil actual",
            description = """
                    Devuelve el contexto del usuario autenticado:
                    - tenantId
                    - userId
                    - roles y permissions
                    
                    Nota: tenant se deriva del token/sesión. No acepta `X-Tenant-Key`.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthDtos.MeResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "tenantId": "1c8dbd77-0f6d-41d0-9f40-5c3f6ab2c210",
                                      "userId": "b9f2f1b4-0d84-4b89-9f10-1c3f0e4c7b8a",
                                      "fullName": "Owner Admin",
                                      "email": "owner@acme.com",
                                      "status": "ACTIVE",
                                      "roles": ["ADMIN"],
                                      "permissions": [
                                        "identity.login",
                                        "identity.read",
                                        "identity.users.manage",
                                        "identity.roles.manage"
                                      ]
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", description = "No autenticado / token inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Autenticado pero sin permisos (si aplicara)", content = @Content)
    })
    @GetMapping("/me")
    public AuthDtos.MeResponse me(Authentication authentication) {
        IdentityPrincipal p = (IdentityPrincipal) authentication.getPrincipal();
        return auth.me(p.tenantId(), p.userId());
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private static String resolveToken(String authHeader, String cookieToken) {
        String bearer = extractBearer(authHeader);
        if (bearer != null) return bearer;
        if (cookieToken != null && !cookieToken.isBlank()) return cookieToken.trim();
        return null;
    }

    private static String extractClientIp(String xff) {
        if (xff == null || xff.isBlank()) return null;
        String[] parts = xff.split(",");
        return (parts.length == 0) ? null : parts[0].trim();
    }

    private static String extractBearer(String header) {
        if (header == null) return null;
        if (!header.startsWith("Bearer ")) return null;
        String token = header.substring(7).trim();
        return token.isBlank() ? null : token;
    }

    private static void setSessionCookie(HttpServletResponse res, String token, boolean rememberMe) {
        Cookie c = new Cookie(COOKIE_NAME, token);
        c.setHttpOnly(true);
        c.setPath("/");
        c.setMaxAge(rememberMe ? 60 * 60 * 24 * 30 : 60 * 60 * 8);
        // c.setSecure(true); // si estás en HTTPS
        res.addCookie(c);
    }

    private static void clearSessionCookie(HttpServletResponse res) {
        Cookie c = new Cookie(COOKIE_NAME, "");
        c.setHttpOnly(true);
        c.setPath("/");
        c.setMaxAge(0);
        // c.setSecure(true);
        res.addCookie(c);
    }
}
