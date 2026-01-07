package com.scalaris.modules.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.util.Set;
import java.util.UUID;

public final class AdminDtos {
    private AdminDtos(){}

    // -------- Roles --------

    @Schema(description = "Crear/actualizar rol")
    public record RoleUpsertRequest(
            @Schema(example = "ADMIN") @NotBlank @Size(max = 80) String name,

            @Schema(
                    description = "Códigos de permiso (lower.dot). Ej: identity.users.manage",
                    example = "[\"identity.read\",\"identity.users.manage\",\"identity.roles.manage\",\"identity.login\"]"
            )
            Set<@NotBlank @Size(max = 80) String> permissions
    ) {}

    public record RoleResponse(UUID id, String name, Set<String> permissions) {}

    // -------- Users --------

    @Schema(description = "Crear usuario (CUS-02). password opcional => PENDING")
    public record UserCreateRequest(
            @NotBlank @Email @Schema(example = "user@acme.com") String email,
            @Size(min = 8, max = 100) @Schema(nullable = true) String password,
            @NotBlank @Size(max = 120) String fullName,
            @Size(max = 40) String phone,
            @NotEmpty Set<UUID> roleIds
    ) {}

    @Schema(description = "Editar usuario (CUS-03) - parcial. NO incluye roles.")
    public record UserUpdateRequest(
            @Email @Schema(nullable = true, example = "user@acme.com") String email,
            @Size(max = 120) String fullName,
            @Size(max = 40) String phone
    ) {}

    @Schema(description = "Asignar roles (CUS-04)")
    public record AssignRolesRequest(
            @NotEmpty Set<UUID> roleIds
    ) {}

//    @Schema(description = "Cambiar status (opcional si querés endpoint explícito)")
//    public enum UserStatus { PENDING, ACTIVE, INACTIVE, LOCKED }

    public record UserResponse(
            UUID id,
            String email,
            String fullName,
            String phone,
            String status,
            Set<RoleLite> roles
    ) {}

    public record RoleLite(UUID id, String name) {}
}
