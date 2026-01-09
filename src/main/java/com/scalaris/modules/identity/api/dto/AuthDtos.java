package com.scalaris.modules.identity.api.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.util.Set;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos(){}

    private static final String PERMISSION_REGEX = "^[a-z0-9]+(\\.[a-z0-9]+)+$";

    @Schema(
            name = "LoginRequest",
            description = "Request de login (CUS-01)."
    )
    public record LoginRequest(
            @Schema(description = "Email del usuario.", example = "owner@acme.com", maxLength = 254)
            @NotBlank @Email @Size(max = 254)
            String email,

            @Schema(description = "Password del usuario.", example = "Str0ngPass!", accessMode = Schema.AccessMode.WRITE_ONLY)
            @NotBlank
            String password,

            @Schema(description = "Extiende duración de sesión (remember me).", defaultValue = "false", example = "true", nullable = true)
            Boolean rememberMe
    ) {}

    @Schema(
            name = "LoginResponse",
            description = """
                    Respuesta de login.
                    
                    - `sessionToken` es token opaco (guardar como secreto).
                    - `permissions` viene canonicalizado.
                    """
    )
    public record LoginResponse(
            @Schema(description = "Tenant ID", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
            UUID tenantId,

            @Schema(description = "User ID", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
            UUID userId,

            @Schema(description = "Nombre completo", example = "Owner Admin", accessMode = Schema.AccessMode.READ_ONLY)
            String fullName,

            @Schema(description = "Email", example = "owner@acme.com", accessMode = Schema.AccessMode.READ_ONLY)
            String email,

            @Schema(
                    description = "Token de sesión opaco. Usar en Bearer o cookie HttpOnly SESSION.",
                    example = "y8yYJ4i9v8E8bJmHcT3o8f7oJm1Jf8wJt8lYbQ",
                    accessMode = Schema.AccessMode.READ_ONLY
            )
            String sessionToken,

            @ArraySchema(
                    schema = @Schema(description = "Nombres de roles.", example = "ADMIN"),
                    uniqueItems = true
            )
            Set<String> roles,

            @ArraySchema(
                    schema = @Schema(description = "Permisos efectivos (lower.dot).", example = "identity.read", pattern = PERMISSION_REGEX),
                    uniqueItems = true
            )
            Set<String> permissions
    ) {}

    @Schema(
            name = "MeResponse",
            description = "Contexto del usuario autenticado."
    )
    public record MeResponse(
            @Schema(description = "Tenant ID", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
            UUID tenantId,

            @Schema(description = "User ID", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
            UUID userId,

            @Schema(description = "Nombre completo", example = "Owner Admin", accessMode = Schema.AccessMode.READ_ONLY)
            String fullName,

            @Schema(description = "Email", example = "owner@acme.com", accessMode = Schema.AccessMode.READ_ONLY)
            String email,

            @Schema(
                    description = "Estado del usuario.",
                    example = "ACTIVE",
                    allowableValues = {"PENDING", "ACTIVE", "INACTIVE", "LOCKED"},
                    accessMode = Schema.AccessMode.READ_ONLY
            )
            String status,

            @ArraySchema(schema = @Schema(example = "ADMIN"), uniqueItems = true)
            Set<String> roles,

            @ArraySchema(schema = @Schema(example = "identity.read", pattern = PERMISSION_REGEX), uniqueItems = true)
            Set<String> permissions
    ) {}

    @Schema(name = "PasswordResetRequest", description = "Solicita reset de contraseña. Respuesta siempre OK (no revela existencia).")
    public record PasswordResetRequest(
            @Schema(description = "Email del usuario.", example = "owner@acme.com", maxLength = 254)
            @NotBlank @Email @Size(max = 254)
            String email
    ) {}

    @Schema(name = "PasswordResetConfirm", description = "Confirma reset de contraseña con token opaco.")
    public record PasswordResetConfirm(
            @Schema(description = "Token opaco de reset.", example = "r3s3tT0k3nOpaqueHere", accessMode = Schema.AccessMode.WRITE_ONLY)
            @NotBlank
            String token,

            @Schema(description = "Nuevo password.", example = "N3wStr0ngPass!", minLength = 8, maxLength = 100, accessMode = Schema.AccessMode.WRITE_ONLY)
            @NotBlank @Size(min = 8, max = 100)
            String newPassword
    ) {}
}
