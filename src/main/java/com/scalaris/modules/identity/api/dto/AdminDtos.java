package com.scalaris.modules.identity.api.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.Set;
import java.util.UUID;

public final class AdminDtos {
    private AdminDtos(){}

    // Regex canónica "lower.dot" (sin wildcards). Ajustable si necesitás guiones, etc.
    private static final String PERMISSION_REGEX = "^[a-z0-9]+(\\.[a-z0-9]+)+$";

    // -------- Roles --------

    @Schema(
            name = "RoleUpsertRequest",
            description = """
                    Crear/actualizar rol.
                    
                    - `name` se normaliza en backend (por ej. uppercase).
                    - `permissions` son códigos estilo `lower.dot` (ej: `identity.users.manage`).
                    """
    )
    public record RoleUpsertRequest(
            @Schema(
                    description = "Nombre del rol (se normaliza server-side).",
                    example = "ADMIN",
                    maxLength = 80
            )
            @NotBlank @Size(max = 80)
            String name,

            @ArraySchema(
                    schema = @Schema(
                            description = "Permisos del rol (lower.dot). Si viene vacío, el rol queda sin permisos.",
                            example = "identity.users.manage",
                            maxLength = 80,
                            pattern = PERMISSION_REGEX
                    ),
                    minItems = 0,
                    uniqueItems = true,
                    arraySchema = @Schema(
                            description = "Lista de permisos del rol.",
                            example = "[\"identity.read\",\"identity.users.manage\",\"identity.roles.manage\",\"identity.login\"]"
                    )
            )
            Set<
                    @NotBlank
                    @Size(max = 80)
                    @Pattern(regexp = PERMISSION_REGEX, message = "permission must be lower.dot (e.g. identity.users.manage)")
                            String
                    > permissions
    ) {}

    @Schema(
            name = "RoleResponse",
            description = "Rol con permisos."
    )
    public record RoleResponse(
            @Schema(description = "ID del rol", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
            UUID id,

            @Schema(description = "Nombre del rol", example = "ADMIN", accessMode = Schema.AccessMode.READ_ONLY)
            String name,

            @ArraySchema(
                    schema = @Schema(
                            description = "Permisos del rol (canonicalizados).",
                            example = "identity.users.manage",
                            pattern = PERMISSION_REGEX
                    ),
                    uniqueItems = true
            )
            Set<String> permissions
    ) {}

    // -------- Users --------

    @Schema(
            name = "UserCreateRequest",
            description = """
                    Crear usuario (CUS-02).
                    
                    - `password` es opcional: si no viene => el usuario puede quedar PENDING (según tu dominio).
                    - `roleIds` obligatorio (no se permite usuario sin rol).
                    """
    )
    public record UserCreateRequest(
            @Schema(description = "Email del usuario (único por tenant).", example = "user@acme.com", maxLength = 254)
            @NotBlank @Email @Size(max = 254)
            String email,

            @Schema(
                    description = "Password inicial (opcional). Si no se envía, el usuario puede quedar en PENDING.",
                    example = "Str0ngPass!",
                    minLength = 8,
                    maxLength = 100,
                    nullable = true,
                    accessMode = Schema.AccessMode.WRITE_ONLY
            )
            @Size(min = 8, max = 100)
            String password,

            @Schema(description = "Nombre completo.", example = "Juan Pérez", minLength = 3, maxLength = 120)
            @NotBlank @Size(min = 3, max = 120)
            String fullName,

            @Schema(
                    description = "Teléfono (formato libre, recomendado E.164).",
                    example = "+54 9 11 2345-6789",
                    maxLength = 40,
                    nullable = true
            )
            @Size(max = 40)
            String phone,

            @ArraySchema(
                    schema = @Schema(description = "IDs de roles a asignar.", format = "uuid"),
                    minItems = 1,
                    uniqueItems = true
            )
            @NotEmpty
            Set<@NotNull UUID> roleIds
    ) {}

    @Schema(
            name = "UserUpdateRequest",
            description = """
                    Editar usuario (CUS-03) - parcial.
                    
                    - Campos opcionales (null => no cambia).
                    - NO incluye roles.
                    """
    )
    public record UserUpdateRequest(
            @Schema(description = "Nuevo email (único por tenant).", example = "user@acme.com", maxLength = 254, nullable = true)
            @Email @Size(max = 254)
            String email,

            @Schema(description = "Nuevo nombre completo.", example = "Juan Pérez", maxLength = 120, nullable = true)
            @Size(min = 3, max = 120)
            String fullName,

            @Schema(description = "Nuevo teléfono.", example = "+54 9 11 2345-6789", maxLength = 40, nullable = true)
            @Size(max = 40)
            String phone
    ) {}

    @Schema(
            name = "AssignRolesRequest",
            description = "Asignar roles a usuario (CUS-04)."
    )
    public record AssignRolesRequest(
            @ArraySchema(
                    schema = @Schema(description = "IDs de roles.", format = "uuid"),
                    minItems = 1,
                    uniqueItems = true
            )
            @NotEmpty
            Set<@NotNull UUID> roleIds
    ) {}

    @Schema(
            name = "UserResponse",
            description = "Usuario con roles (sin permisos expandidos)."
    )
    public record UserResponse(
            @Schema(description = "ID del usuario", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
            UUID id,

            @Schema(description = "Email", example = "user@acme.com", accessMode = Schema.AccessMode.READ_ONLY)
            String email,

            @Schema(description = "Nombre completo", example = "Juan Pérez", accessMode = Schema.AccessMode.READ_ONLY)
            String fullName,

            @Schema(description = "Teléfono", example = "+54 9 11 2345-6789", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
            String phone,

            @Schema(
                    description = "Estado del usuario.",
                    example = "ACTIVE",
                    allowableValues = {"PENDING", "ACTIVE", "INACTIVE", "LOCKED"},
                    accessMode = Schema.AccessMode.READ_ONLY
            )
            String status,

            @ArraySchema(
                    schema = @Schema(implementation = RoleLite.class),
                    uniqueItems = true
            )
            Set<@Valid RoleLite> roles
    ) {}

    @Schema(name = "RoleLite", description = "Rol resumido para responses de usuarios.")
    public record RoleLite(
            @Schema(description = "ID del rol", format = "uuid")
            UUID id,

            @Schema(description = "Nombre del rol", example = "ADMIN")
            String name
    ) {}
}
