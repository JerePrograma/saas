package com.scalaris.modules.identity.api;

import com.scalaris.bootstrap.security.SessionAuthFilter.IdentityPrincipal;
import com.scalaris.modules.identity.api.dto.AdminDtos;
import com.scalaris.modules.identity.application.AdminAppService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Hidden // opcional: no lo muestres en Swagger
@Tag(name = "Identity Admin (Legacy)", description = "Compat temporal. Migrar a /identity/roles y /identity/users.")
@RestController
@RequestMapping("/api/v1/identity/admin") // <- clave: NO colisiona
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminAppService admin;

    public AdminController(AdminAppService admin) {
        this.admin = admin;
    }

    @PreAuthorize("hasAuthority(T(com.scalaris.shared.security.Permissions).IDENTITY_ROLES_MANAGE)")
    @PostMapping("/roles")
    public AdminDtos.RoleResponse createRole(Authentication authentication,
                                             @Valid @RequestBody AdminDtos.RoleUpsertRequest req) {
        IdentityPrincipal p = (IdentityPrincipal) authentication.getPrincipal();
        return admin.createRole(p.tenantId(), p.userId(), req);
    }

    @PreAuthorize("hasAuthority(T(com.scalaris.shared.security.Permissions).IDENTITY_ROLES_MANAGE)")
    @PutMapping("/roles/{roleId}")
    public AdminDtos.RoleResponse updateRole(Authentication authentication,
                                             @PathVariable UUID roleId,
                                             @Valid @RequestBody AdminDtos.RoleUpsertRequest req) {
        IdentityPrincipal p = (IdentityPrincipal) authentication.getPrincipal();
        return admin.updateRole(p.tenantId(), p.userId(), roleId, req);
    }

    @PreAuthorize("hasAnyAuthority(T(com.scalaris.shared.security.Permissions).IDENTITY_READ, T(com.scalaris.shared.security.Permissions).IDENTITY_ROLES_MANAGE)")
    @GetMapping("/roles")
    public List<AdminDtos.RoleResponse> listRoles(Authentication authentication) {
        IdentityPrincipal p = (IdentityPrincipal) authentication.getPrincipal();
        return admin.listRoles(p.tenantId());
    }

    @PreAuthorize("hasAuthority(T(com.scalaris.shared.security.Permissions).IDENTITY_USERS_MANAGE)")
    @PostMapping("/users")
    public AdminDtos.UserResponse createUser(Authentication authentication,
                                             @Valid @RequestBody AdminDtos.UserCreateRequest req) {
        IdentityPrincipal p = (IdentityPrincipal) authentication.getPrincipal();
        return admin.createUser(p.tenantId(), p.userId(), req);
    }

    @PreAuthorize("hasAuthority(T(com.scalaris.shared.security.Permissions).IDENTITY_USERS_MANAGE)")
    @PutMapping("/users/{userId}")
    public AdminDtos.UserResponse updateUser(Authentication authentication,
                                             @PathVariable UUID userId,
                                             @Valid @RequestBody AdminDtos.UserUpdateRequest req) {
        IdentityPrincipal p = (IdentityPrincipal) authentication.getPrincipal();
        return admin.updateUser(p.tenantId(), p.userId(), userId, req);
    }
}
