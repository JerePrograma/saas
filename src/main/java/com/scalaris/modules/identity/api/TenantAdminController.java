package com.scalaris.modules.identity.api;

import com.scalaris.bootstrap.security.SessionAuthFilter.IdentityPrincipal;
import com.scalaris.modules.identity.api.dto.TenantBootstrapDtos;
import com.scalaris.modules.identity.api.dto.TenantDtos;
import com.scalaris.modules.identity.application.TenantAdminAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Tenants (Platform)", description = "CRUD de tenants (nivel plataforma)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/platform/tenants")
public class TenantAdminController {

    private final TenantAdminAppService tenants;

    public TenantAdminController(TenantAdminAppService tenants) {
        this.tenants = tenants;
    }

    @Operation(summary = "Crear tenant")
    @PreAuthorize("hasAuthority(T(com.scalaris.shared.security.Permissions).PLATFORM_TENANTS_MANAGE)")
    @PostMapping
    public TenantDtos.TenantResponse create(Authentication auth,
                                            @Valid @RequestBody TenantDtos.TenantCreateRequest req) {
        UUID actorId = ((IdentityPrincipal) auth.getPrincipal()).userId();
        return tenants.create(actorId, req);
    }

    @Operation(summary = "Bootstrap admin del tenant",
            description = "Crea rol ADMIN con permisos + usuario owner. Solo 1 vez (si ya hay usuarios, falla).")
    @PreAuthorize("hasAuthority(T(com.scalaris.shared.security.Permissions).PLATFORM_TENANTS_MANAGE)")
    @PostMapping("/{tenantId}/bootstrap-admin")
    public TenantBootstrapDtos.BootstrapAdminResponse bootstrapAdmin(Authentication auth,
                                                                     @PathVariable UUID tenantId,
                                                                     @Valid @RequestBody TenantBootstrapDtos.BootstrapAdminRequest req
    ) {
        UUID actorId = ((IdentityPrincipal) auth.getPrincipal()).userId();
        return tenants.bootstrapAdmin(actorId, tenantId, req);
    }

    // ... resto igual
    @Operation(summary = "Actualizar tenant")
    @PreAuthorize("hasAuthority(T(com.scalaris.shared.security.Permissions).PLATFORM_TENANTS_MANAGE)")
    @PutMapping("/{tenantId}")
    public TenantDtos.TenantResponse update(Authentication auth,
                                            @PathVariable UUID tenantId,
                                            @Valid @RequestBody TenantDtos.TenantUpdateRequest req) {
        UUID actorId = ((IdentityPrincipal) auth.getPrincipal()).userId();
        return tenants.update(actorId, tenantId, req);
    }

    @Operation(summary = "Obtener tenant")
    @PreAuthorize("hasAuthority(T(com.scalaris.shared.security.Permissions).PLATFORM_TENANTS_MANAGE)")
    @GetMapping("/{tenantId}")
    public TenantDtos.TenantResponse get(@PathVariable UUID tenantId) {
        return tenants.get(tenantId);
    }

    @Operation(summary = "Listar tenants")
    @PreAuthorize("hasAuthority(T(com.scalaris.shared.security.Permissions).PLATFORM_TENANTS_MANAGE)")
    @GetMapping
    public List<TenantDtos.TenantResponse> list() {
        return tenants.list();
    }

    @Operation(summary = "Cambiar status del tenant")
    @PreAuthorize("hasAuthority(T(com.scalaris.shared.security.Permissions).PLATFORM_TENANTS_MANAGE)")
    @PostMapping("/{tenantId}/status")
    public TenantDtos.TenantResponse changeStatus(Authentication auth,
                                                  @PathVariable UUID tenantId,
                                                  @Valid @RequestBody TenantDtos.TenantChangeStatusRequest req) {
        UUID actorId = ((IdentityPrincipal) auth.getPrincipal()).userId();
        return tenants.changeStatus(actorId, tenantId, req);
    }

    @Operation(summary = "Cambiar plan del tenant")
    @PreAuthorize("hasAuthority(T(com.scalaris.shared.security.Permissions).PLATFORM_TENANTS_MANAGE)")
    @PostMapping("/{tenantId}/plan")
    public TenantDtos.TenantResponse changePlan(Authentication auth,
                                                @PathVariable UUID tenantId,
                                                @Valid @RequestBody TenantDtos.TenantChangePlanRequest req) {
        UUID actorId = ((IdentityPrincipal) auth.getPrincipal()).userId();
        return tenants.changePlan(actorId, tenantId, req);
    }
}
