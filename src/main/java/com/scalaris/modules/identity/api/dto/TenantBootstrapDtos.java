package com.scalaris.modules.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.util.Set;
import java.util.UUID;

public final class TenantBootstrapDtos {
    private TenantBootstrapDtos(){}

    public record BootstrapAdminRequest(
            @NotBlank @Email @Schema(example = "owner@acme.com") String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank @Size(max = 120) String fullName
    ) {}

    public record BootstrapAdminResponse(
            UUID tenantId,
            UUID ownerUserId,
            UUID adminRoleId,
            Set<String> permissions
    ) {}
}
