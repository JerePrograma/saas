package com.scalaris.modules.identity.api;

import com.scalaris.bootstrap.security.SessionAuthFilter.IdentityPrincipal;
import com.scalaris.modules.identity.api.dto.AdminDtos;
import com.scalaris.modules.identity.application.AdminAppService;
import com.scalaris.shared.security.annotations.RequireIdentityRead;
import com.scalaris.shared.security.annotations.RequireIdentityRolesManage;
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
@RequestMapping("/api/v1/identity/roles")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "02 - Identity - Roles")
public class IdentityRolesController {

    private final AdminAppService admin;

    public IdentityRolesController(AdminAppService admin) {
        this.admin = admin;
    }

    @Operation(summary = "Crear rol")
    @RequireIdentityRolesManage
    @PostMapping
    public AdminDtos.RoleResponse createRole(
            @Parameter(hidden = true) @AuthenticationPrincipal IdentityPrincipal p,
            @Valid @RequestBody AdminDtos.RoleUpsertRequest req
    ) {
        return admin.createRole(p.tenantId(), p.userId(), req);
    }

    @Operation(summary = "Actualizar rol")
    @RequireIdentityRolesManage
    @PutMapping("/{roleId}")
    public AdminDtos.RoleResponse updateRole(
            @Parameter(hidden = true) @AuthenticationPrincipal IdentityPrincipal p,
            @PathVariable UUID roleId,
            @Valid @RequestBody AdminDtos.RoleUpsertRequest req
    ) {
        return admin.updateRole(p.tenantId(), p.userId(), roleId, req);
    }

    @Operation(summary = "Listar roles")
    @RequireIdentityRead
    @GetMapping
    public List<AdminDtos.RoleResponse> listRoles(
            @Parameter(hidden = true) @AuthenticationPrincipal IdentityPrincipal p
    ) {
        return admin.listRoles(p.tenantId());
    }
}
