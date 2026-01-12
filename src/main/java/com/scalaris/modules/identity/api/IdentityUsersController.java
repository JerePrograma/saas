package com.scalaris.modules.identity.api;

import com.scalaris.bootstrap.security.SessionAuthFilter.IdentityPrincipal;
import com.scalaris.modules.identity.api.dto.AdminDtos;
import com.scalaris.modules.identity.application.AdminAppService;
import com.scalaris.shared.security.annotations.RequireIdentityRead;
import com.scalaris.shared.security.annotations.RequireIdentityRolesManage;
import com.scalaris.shared.security.annotations.RequireIdentityUsersManage;
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
@RequestMapping("/api/v1/identity/users")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "02 - Identity - Users")
public class IdentityUsersController {

    private final AdminAppService admin;

    public IdentityUsersController(AdminAppService admin) {
        this.admin = admin;
    }

    @Operation(summary = "Crear usuario (CUS-02)")
    @RequireIdentityUsersManage
    @PostMapping
    public AdminDtos.UserResponse createUser(
            @Parameter(hidden = true) @AuthenticationPrincipal IdentityPrincipal p,
            @Valid @RequestBody AdminDtos.UserCreateRequest req
    ) {
        return admin.createUser(p.tenantId(), p.userId(), req);
    }

    @Operation(summary = "Editar usuario (CUS-03)")
    @RequireIdentityUsersManage
    @PutMapping("/{userId}")
    public AdminDtos.UserResponse updateUser(
            @Parameter(hidden = true) @AuthenticationPrincipal IdentityPrincipal p,
            @PathVariable UUID userId,
            @Valid @RequestBody AdminDtos.UserUpdateRequest req
    ) {
        return admin.updateUser(p.tenantId(), p.userId(), userId, req);
    }

    @Operation(summary = "Listar usuarios")
    @RequireIdentityRead
    @GetMapping
    public List<AdminDtos.UserResponse> listUsers(
            @Parameter(hidden = true) @AuthenticationPrincipal IdentityPrincipal p
    ) {
        return admin.listUsers(p.tenantId(), p.userId());
    }

    @Operation(summary = "Obtener usuario por ID")
    @RequireIdentityRead
    @GetMapping("/{userId}")
    public AdminDtos.UserResponse getUser(
            @Parameter(hidden = true) @AuthenticationPrincipal IdentityPrincipal p,
            @PathVariable UUID userId
    ) {
        return admin.getUser(p.tenantId(), p.userId(), userId);
    }

    @Operation(summary = "Desactivar usuario (CUS-05)")
    @RequireIdentityUsersManage
    @PostMapping("/{userId}/deactivate")
    public void deactivateUser(
            @Parameter(hidden = true) @AuthenticationPrincipal IdentityPrincipal p,
            @PathVariable UUID userId
    ) {
        admin.deactivateUser(p.tenantId(), p.userId(), userId);
    }

    @Operation(summary = "Asignar roles a usuario (CUS-04)")
    @RequireIdentityRolesManage
    @PutMapping("/{userId}/roles")
    public AdminDtos.UserResponse assignRoles(
            @Parameter(hidden = true) @AuthenticationPrincipal IdentityPrincipal p,
            @PathVariable UUID userId,
            @Valid @RequestBody AdminDtos.AssignRolesRequest req
    ) {
        return admin.assignRoles(p.tenantId(), p.userId(), userId, req);
    }
}
