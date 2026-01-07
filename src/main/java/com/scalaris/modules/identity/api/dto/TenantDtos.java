// ============================================================================
// modules/identity/api/dto/TenantDtos.java
// ============================================================================
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

    @Schema(name = "TenantCreateRequest")
    public record TenantCreateRequest(
            @NotBlank
            @Size(max = 120)
            @Schema(example = "Mueblería Juan")
            String name,

            @NotBlank
            @Schema(description = "BASIC | PRO | ENTERPRISE", example = "BASIC")
            String plan,

            @Schema(
                    description = "Settings arbitrarios del tenant (se persisten en settings_json).",
                    example = "{\"modules\":{\"inventory\":true}}"
            )
            JsonNode settings
    ) {}

    @Schema(name = "TenantUpdateRequest")
    public record TenantUpdateRequest(
            @NotBlank
            @Size(max = 120)
            @Schema(example = "Mueblería Juan (Sucursal Centro)")
            String name,

            @Schema(
                    description = "Settings arbitrarios del tenant (se persisten en settings_json).",
                    example = "{\"limits\":{\"users\":10}}"
            )
            JsonNode settings
    ) {}

    @Schema(name = "TenantChangeStatusRequest")
    public record TenantChangeStatusRequest(
            @NotBlank
            @Schema(description = "ACTIVE | SUSPENDED | CANCELED", example = "SUSPENDED")
            String status
    ) {}

    @Schema(name = "TenantChangePlanRequest")
    public record TenantChangePlanRequest(
            @NotBlank
            @Schema(description = "BASIC | PRO | ENTERPRISE", example = "PRO")
            String plan
    ) {}

    // -----------------------------
    // Responses
    // -----------------------------

    @Schema(name = "TenantResponse")
    public record TenantResponse(
            UUID id,
            String name,
            String status,
            String plan,

            @Schema(
                    description = "Settings del tenant como objeto JSON (derivado de settings_json).",
                    example = "{\"modules\":{\"inventory\":true}}"
            )
            JsonNode settings
    ) {}
}
