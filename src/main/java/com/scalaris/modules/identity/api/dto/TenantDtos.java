package com.scalaris.modules.identity.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class TenantDtos {
    private TenantDtos(){}

    // -----------------------------
    // Requests
    // -----------------------------

    @Schema(
            name = "TenantCreateRequest",
            description = "Crear tenant (nivel plataforma)."
    )
    public record TenantCreateRequest(
            @Schema(description = "Nombre del tenant.", example = "Mueblería Juan", maxLength = 120)
            @NotBlank @Size(max = 120)
            String name,

            @Schema(
                    description = "Plan inicial del tenant.",
                    example = "BASIC",
                    allowableValues = {"BASIC", "PRO", "ENTERPRISE"}
            )
            @NotBlank
            String plan,

            @Schema(
                    description = "Settings arbitrarios del tenant (se persisten en settings_json).",
                    example = "{\"modules\":{\"inventory\":true}}",
                    nullable = true
            )
            JsonNode settings
    ) {}

    @Schema(
            name = "TenantUpdateRequest",
            description = "Actualizar tenant (no cambia slug automáticamente)."
    )
    public record TenantUpdateRequest(
            @Schema(description = "Nuevo nombre del tenant.", example = "Mueblería Juan (Sucursal Centro)", maxLength = 120)
            @NotBlank @Size(max = 120)
            String name,

            @Schema(
                    description = "Settings arbitrarios del tenant (se persisten en settings_json).",
                    example = "{\"limits\":{\"users\":10}}",
                    nullable = true
            )
            JsonNode settings
    ) {}

    @Schema(name = "TenantChangeStatusRequest", description = "Cambio de status del tenant.")
    public record TenantChangeStatusRequest(
            @Schema(
                    description = "Nuevo status del tenant.",
                    example = "SUSPENDED",
                    allowableValues = {"ACTIVE", "SUSPENDED", "CANCELED"}
            )
            @NotBlank
            String status
    ) {}

    @Schema(name = "TenantChangePlanRequest", description = "Cambio de plan del tenant.")
    public record TenantChangePlanRequest(
            @Schema(
                    description = "Nuevo plan del tenant.",
                    example = "PRO",
                    allowableValues = {"BASIC", "PRO", "ENTERPRISE"}
            )
            @NotBlank
            String plan
    ) {}

    // -----------------------------
    // Responses
    // -----------------------------

    @Schema(
            name = "TenantResponse",
            description = "Tenant (nivel plataforma)."
    )
    public record TenantResponse(
            @Schema(description = "Tenant ID", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
            UUID id,

            @Schema(description = "Nombre", example = "Mueblería Juan", accessMode = Schema.AccessMode.READ_ONLY)
            String name,

            @Schema(
                    description = "Status",
                    example = "ACTIVE",
                    allowableValues = {"ACTIVE", "SUSPENDED", "CANCELED"},
                    accessMode = Schema.AccessMode.READ_ONLY
            )
            String status,

            @Schema(
                    description = "Plan",
                    example = "BASIC",
                    allowableValues = {"BASIC", "PRO", "ENTERPRISE"},
                    accessMode = Schema.AccessMode.READ_ONLY
            )
            String plan,

            @Schema(
                    description = "Slug estable del tenant (para `X-Tenant-Key`).",
                    example = "muebleria-juan",
                    nullable = true,
                    accessMode = Schema.AccessMode.READ_ONLY
            )
            String slug,

            @Schema(
                    description = "Settings del tenant como objeto JSON (derivado de settings_json).",
                    example = "{\"modules\":{\"inventory\":true}}",
                    accessMode = Schema.AccessMode.READ_ONLY
            )
            JsonNode settings
    ) {}
}
