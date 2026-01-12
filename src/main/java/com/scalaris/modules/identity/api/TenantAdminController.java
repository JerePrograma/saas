package com.scalaris.modules.identity.api;

import com.scalaris.bootstrap.security.SessionAuthFilter.IdentityPrincipal;
import com.scalaris.modules.identity.api.dto.TenantBootstrapDtos;
import com.scalaris.modules.identity.api.dto.TenantDtos;
import com.scalaris.modules.identity.application.TenantAdminAppService;
import com.scalaris.shared.security.annotations.RequirePlatformTenantsManage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/platform/tenants")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "03 - Platform - Tenants")
public class TenantAdminController {

    private final TenantAdminAppService tenants;

    public TenantAdminController(TenantAdminAppService tenants) {
        this.tenants = tenants;
    }

    @Operation(summary = "Crear tenant")
    @RequirePlatformTenantsManage
    @PostMapping
    public TenantDtos.TenantResponse create(
            @Parameter(hidden = true) @AuthenticationPrincipal IdentityPrincipal p,
            @Valid @RequestBody TenantDtos.TenantCreateRequest req
    ) {
        return tenants.create(p.userId(), req);
    }

    @Operation(summary = "Bootstrap admin del tenant")
    @RequirePlatformTenantsManage
    @PostMapping("/{tenantId}/bootstrap-admin")
    public TenantBootstrapDtos.BootstrapAdminResponse bootstrapAdmin(
            @Parameter(hidden = true) @AuthenticationPrincipal IdentityPrincipal p,
            @PathVariable UUID tenantId,
            @Valid @RequestBody TenantBootstrapDtos.BootstrapAdminRequest req
    ) {
        return tenants.bootstrapAdmin(p.userId(), tenantId, req);
    }

    @Operation(summary = "Actualizar tenant")
    @RequirePlatformTenantsManage
    @PutMapping("/{tenantId}")
    public TenantDtos.TenantResponse update(
            @Parameter(hidden = true) @AuthenticationPrincipal IdentityPrincipal p,
            @PathVariable UUID tenantId,
            @Valid @RequestBody TenantDtos.TenantUpdateRequest req
    ) {
        return tenants.update(p.userId(), tenantId, req);
    }

    @Operation(summary = "Obtener tenant")
    @RequirePlatformTenantsManage
    @GetMapping("/{tenantId}")
    public TenantDtos.TenantResponse get(@PathVariable UUID tenantId) {
        return tenants.get(tenantId);
    }

    @Operation(summary = "Listar tenants")
    @RequirePlatformTenantsManage
    @GetMapping
    public List<TenantDtos.TenantResponse> list() {
        return tenants.list();
    }

    @Operation(summary = "Cambiar status del tenant")
    @RequirePlatformTenantsManage
    @PostMapping("/{tenantId}/status")
    public TenantDtos.TenantResponse changeStatus(
            @Parameter(hidden = true) @AuthenticationPrincipal IdentityPrincipal p,
            @PathVariable UUID tenantId,
            @Valid @RequestBody TenantDtos.TenantChangeStatusRequest req
    ) {
        return tenants.changeStatus(p.userId(), tenantId, req);
    }

    @Operation(summary = "Cambiar plan del tenant")
    @RequirePlatformTenantsManage
    @PostMapping("/{tenantId}/plan")
    public TenantDtos.TenantResponse changePlan(
            @Parameter(hidden = true) @AuthenticationPrincipal IdentityPrincipal p,
            @PathVariable UUID tenantId,
            @Valid @RequestBody TenantDtos.TenantChangePlanRequest req
    ) {
        return tenants.changePlan(p.userId(), tenantId, req);
    }
}
