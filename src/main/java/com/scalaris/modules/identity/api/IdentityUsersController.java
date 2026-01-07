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

@Tag(name = "Identity Users", description = "Alta/edición/desactivación de usuarios (CUS-02/03/05).")
@RestController
@RequestMapping("/api/v1/identity/users")
@SecurityRequirement(name = "bearerAuth")
public class IdentityUsersController {

    private final AdminAppService admin;

    public IdentityUsersController(AdminAppService admin) {
        this.admin = admin;
    }

    // CUS-02
    @PreAuthorize("hasAuthority(T(com.scalaris.shared.security.Permissions).IDENTITY_USERS_MANAGE)")
    @PostMapping
    public AdminDtos.UserResponse createUser(Authentication authentication,
                                             @Valid @RequestBody AdminDtos.UserCreateRequest req) {
        IdentityPrincipal p = (IdentityPrincipal) authentication.getPrincipal();
        return admin.createUser(p.tenantId(), p.userId(), req);
    }

    // CUS-03 (NO roles)
    @PreAuthorize("hasAuthority(T(com.scalaris.shared.security.Permissions).IDENTITY_USERS_MANAGE)")
    @PutMapping("/{userId}")
    public AdminDtos.UserResponse updateUser(Authentication authentication,
                                             @PathVariable UUID userId,
                                             @Valid @RequestBody AdminDtos.UserUpdateRequest req) {
        IdentityPrincipal p = (IdentityPrincipal) authentication.getPrincipal();
        return admin.updateUser(p.tenantId(), p.userId(), userId, req);
    }

    // "Ver usuarios" (matriz: identity.read)
    @PreAuthorize("hasAnyAuthority(T(com.scalaris.shared.security.Permissions).IDENTITY_READ, T(com.scalaris.shared.security.Permissions).IDENTITY_USERS_MANAGE)")
    @GetMapping
    public List<AdminDtos.UserResponse> listUsers(Authentication authentication) {
        IdentityPrincipal p = (IdentityPrincipal) authentication.getPrincipal();
        return admin.listUsers(p.tenantId(), p.userId());
    }

    @PreAuthorize("hasAnyAuthority(T(com.scalaris.shared.security.Permissions).IDENTITY_READ, T(com.scalaris.shared.security.Permissions).IDENTITY_USERS_MANAGE)")
    @GetMapping("/{userId}")
    public AdminDtos.UserResponse getUser(Authentication authentication, @PathVariable UUID userId) {
        IdentityPrincipal p = (IdentityPrincipal) authentication.getPrincipal();
        return admin.getUser(p.tenantId(), p.userId(), userId);
    }

    // CUS-05 (endpoint explícito y auditable)
    @PreAuthorize("hasAuthority(T(com.scalaris.shared.security.Permissions).IDENTITY_USERS_MANAGE)")
    @PostMapping("/{userId}/deactivate")
    public void deactivateUser(Authentication authentication, @PathVariable UUID userId) {
        IdentityPrincipal p = (IdentityPrincipal) authentication.getPrincipal();
        admin.deactivateUser(p.tenantId(), p.userId(), userId);
    }
}
