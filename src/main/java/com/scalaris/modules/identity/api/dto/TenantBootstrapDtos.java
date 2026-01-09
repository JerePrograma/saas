package com.scalaris.modules.identity.api.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.util.Set;
import java.util.UUID;

public final class TenantBootstrapDtos {
    private TenantBootstrapDtos(){}

    private static final String PERMISSION_REGEX = "^[a-z0-9]+(\\.[a-z0-9]+)+$";

    @Schema(
            name = "BootstrapAdminRequest",
            description = """
                    Bootstrap inicial del tenant:
                    - crea rol ADMIN con permisos base
                    - crea usuario owner
                    - se permite una sola vez (si ya hay usuarios, falla)
                    """
    )
    public record BootstrapAdminRequest(
            @Schema(description = "Email del owner/admin.", example = "owner@acme.com", maxLength = 254)
            @NotBlank @Email @Size(max = 254)
            String email,

            @Schema(description = "Password inicial del owner.", example = "Str0ngPass!", minLength = 8, maxLength = 100, accessMode = Schema.AccessMode.WRITE_ONLY)
            @NotBlank @Size(min = 8, max = 100)
            String password,

            @Schema(description = "Nombre completo del owner.", example = "Owner Admin", minLength = 3, maxLength = 120)
            @NotBlank @Size(min = 3, max = 120)
            String fullName
    ) {}

    @Schema(
            name = "BootstrapAdminResponse",
            description = "Resultado del bootstrap: IDs creados + set de permisos del rol ADMIN."
    )
    public record BootstrapAdminResponse(
            @Schema(description = "Tenant ID", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
            UUID tenantId,

            @Schema(description = "User ID del owner", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
            UUID ownerUserId,

            @Schema(description = "Role ID del rol ADMIN", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
            UUID adminRoleId,

            @ArraySchema(schema = @Schema(example = "identity.users.manage", pattern = PERMISSION_REGEX), uniqueItems = true)
            Set<String> permissions
    ) {}
}
