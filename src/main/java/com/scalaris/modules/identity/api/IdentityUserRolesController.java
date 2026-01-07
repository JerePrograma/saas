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

import java.util.UUID;

@Tag(name = "Identity User Roles", description = "Asignación de roles a usuarios (CUS-04).")
@RestController
@RequestMapping("/api/v1/identity/users/{userId}/roles")
@SecurityRequirement(name = "bearerAuth")
public class IdentityUserRolesController {

    private final AdminAppService admin;

    public IdentityUserRolesController(AdminAppService admin) {
        this.admin = admin;
    }

    @PreAuthorize("hasAuthority(T(com.scalaris.shared.security.Permissions).IDENTITY_ROLES_MANAGE)")
    @PutMapping
    public AdminDtos.UserResponse assignRoles(Authentication authentication,
                                              @PathVariable UUID userId,
                                              @Valid @RequestBody AdminDtos.AssignRolesRequest req) {
        IdentityPrincipal p = (IdentityPrincipal) authentication.getPrincipal();
        return admin.assignRoles(p.tenantId(), p.userId(), userId, req);
    }
}
