package com.scalaris.modules.identity.api;

import com.scalaris.bootstrap.security.SessionAuthFilter.IdentityPrincipal;
import com.scalaris.modules.identity.api.dto.AdminDtos;
import com.scalaris.modules.identity.application.AdminAppService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Identity Roles", description = "Gestión de roles y permisos (CUS-04).")
@RestController
@RequestMapping("/api/v1/identity/roles")
@SecurityRequirement(name = "bearerAuth")
public class IdentityRolesController {

    private final AdminAppService admin;

    public IdentityRolesController(AdminAppService admin) {
        this.admin = admin;
    }

    @PreAuthorize("hasAuthority(T(com.scalaris.shared.security.Permissions).IDENTITY_ROLES_MANAGE)")
    @PostMapping
    public AdminDtos.RoleResponse createRole(Authentication authentication,
                                             @Valid @RequestBody AdminDtos.RoleUpsertRequest req) {
        IdentityPrincipal p = (IdentityPrincipal) authentication.getPrincipal();
        return admin.createRole(p.tenantId(), p.userId(), req);
    }

    @PreAuthorize("hasAuthority(T(com.scalaris.shared.security.Permissions).IDENTITY_ROLES_MANAGE)")
    @PutMapping("/{roleId}")
    public AdminDtos.RoleResponse updateRole(Authentication authentication,
                                             @PathVariable UUID roleId,
                                             @Valid @RequestBody AdminDtos.RoleUpsertRequest req) {
        IdentityPrincipal p = (IdentityPrincipal) authentication.getPrincipal();
        return admin.updateRole(p.tenantId(), p.userId(), roleId, req);
    }

    // Si querés que "ver roles" sea identity.read, dejalo así.
    @PreAuthorize("hasAnyAuthority(T(com.scalaris.shared.security.Permissions).IDENTITY_READ, T(com.scalaris.shared.security.Permissions).IDENTITY_ROLES_MANAGE)")
    @GetMapping
    public List<AdminDtos.RoleResponse> listRoles(Authentication authentication) {
        IdentityPrincipal p = (IdentityPrincipal) authentication.getPrincipal();
        return admin.listRoles(p.tenantId());
    }
}
