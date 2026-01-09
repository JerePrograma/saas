// ============================================================================
// modules/identity/api/IdentityController.java
// ============================================================================
package com.scalaris.modules.identity.api;

import com.scalaris.bootstrap.security.SessionAuthFilter.IdentityPrincipal;
import com.scalaris.modules.identity.api.dto.AdminDtos;
import com.scalaris.modules.identity.application.AdminAppService;
import com.scalaris.shared.security.Permissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(
        name = "Identity",
        description = """
                Gestión de **Usuarios** y **Roles** dentro del tenant actual (derivado del token).
                
                - RBAC: los permisos se asignan **solo por roles** (no hay permisos directos al usuario).
                - Multi-tenant: el tenant se obtiene del principal autenticado (no se aceptan headers de tenant).
                - Referencias CUS:
                  - CUS-02 Alta usuario
                  - CUS-03 Editar usuario
                  - CUS-04 Asignar rol a usuario / Gestión de roles
                  - CUS-05 Desactivar usuario
                """
)
@RestController
@RequestMapping("/api/v1/identity")
@SecurityRequirement(name = "bearerAuth")
public class IdentityController {

    private final AdminAppService admin;

    public IdentityController(AdminAppService admin) {
        this.admin = admin;
    }

    // -------------------- USERS (CUS-02/03/05 + read) --------------------

    @Operation(
            summary = "Crear usuario (CUS-02)",
            description = """
                    Crea un usuario dentro del **tenant actual** y asigna roles iniciales.
                    
                    Reglas clave:
                    - Requiere permiso: `identity.users.manage`.
                    - Email único dentro del tenant.
                    - Debe asignarse al menos un rol inicial.
                    - No se pueden asignar permisos directos al usuario (solo por roles).
                    - Si password viene vacío/null, el usuario puede quedar en estado PENDING (según tu dominio).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario creado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AdminDtos.UserResponse.class),
                            examples = @ExampleObject(name = "UserResponse", value = """
                                    {
                                      "id": "b9f2f1b4-0d84-4b89-9f10-1c3f0e4c7b8a",
                                      "email": "user@acme.com",
                                      "fullName": "Juan Pérez",
                                      "phone": "+54 9 11 5555-5555",
                                      "status": "ACTIVE",
                                      "roles": [
                                        { "id": "f4f35c7b-1d2a-4a1a-9f0c-9b3c0c7b1a11", "name": "SALES" }
                                      ]
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Validación / regla de negocio",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "EMAIL_DUPLICATE", value = """
                                            { "code": "EMAIL_DUPLICATE", "message": "El email ya está registrado." }
                                            """),
                                    @ExampleObject(name = "USER_ROLE_REQUIRED", value = """
                                            { "code": "USER_ROLE_REQUIRED", "message": "No se puede dejar un usuario sin rol" }
                                            """),
                                    @ExampleObject(name = "ROLE_ASSIGN_OUT_OF_BOUNDS", value = """
                                            { "code": "ROLE_ASSIGN_OUT_OF_BOUNDS", "message": "No podés asignar un rol con más permisos que los tuyos" }
                                            """)
                            })),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "code": "UNAUTHORIZED", "message": "Token inválido o faltante" }
                                    """))),
            @ApiResponse(responseCode = "403", description = "Sin permiso identity.users.manage",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "code": "FORBIDDEN", "message": "Falta permiso: identity.users.manage" }
                                    """)))
    })
    @PreAuthorize("hasAuthority(T(com.scalaris.shared.security.Permissions).IDENTITY_USERS_MANAGE)")
    @PostMapping("/users")
    public AdminDtos.UserResponse createUser(
            Authentication authentication,
            @Valid
            @RequestBody(
                    required = true,
                    description = "Datos de alta de usuario + roles iniciales",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AdminDtos.UserCreateRequest.class),
                            examples = {
                                    @ExampleObject(name = "Con password (ACTIVE)", value = """
                                            {
                                              "email": "user@acme.com",
                                              "fullName": "Juan Pérez",
                                              "phone": "+54 9 11 5555-5555",
                                              "password": "Str0ngPass!",
                                              "roleIds": ["f4f35c7b-1d2a-4a1a-9f0c-9b3c0c7b1a11"]
                                            }
                                            """),
                                    @ExampleObject(name = "Sin password (PENDING/INVITE)", value = """
                                            {
                                              "email": "invite@acme.com",
                                              "fullName": "Invitado",
                                              "phone": null,
                                              "password": null,
                                              "roleIds": ["f4f35c7b-1d2a-4a1a-9f0c-9b3c0c7b1a11"]
                                            }
                                            """)
                            }
                    )
            )
            AdminDtos.UserCreateRequest req
    ) {
        IdentityPrincipal p = (IdentityPrincipal) authentication.getPrincipal();
        return admin.createUser(p.tenantId(), p.userId(), req);
    }

    @Operation(
            summary = "Editar usuario (CUS-03)",
            description = """
                    Modifica datos básicos del usuario (perfil/email) dentro del tenant.
                    
                    Reglas clave:
                    - Requiere permiso: `identity.users.manage`.
                    - No puede cambiarse el tenantId.
                    - Email no puede duplicar otro del tenant.
                    - Cambios sensibles (ej: email) pueden invalidar sesiones (según tu app service).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario actualizado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AdminDtos.UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validación / regla de negocio",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "EMAIL_DUPLICATE", value = """
                                            { "code": "EMAIL_DUPLICATE", "message": "El email ya está registrado." }
                                            """),
                                    @ExampleObject(name = "USER_DELETED", value = """
                                            { "code": "USER_DELETED", "message": "No se puede editar un usuario eliminado" }
                                            """)
                            })),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Sin permiso identity.users.manage",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "code": "NOT_FOUND", "message": "Usuario no encontrado" }
                                    """)))
    })
    @PreAuthorize("hasAuthority(T(com.scalaris.shared.security.Permissions).IDENTITY_USERS_MANAGE)")
    @PutMapping("/users/{userId}")
    public AdminDtos.UserResponse updateUser(
            Authentication authentication,
            @Parameter(
                    name = "userId",
                    in = ParameterIn.PATH,
                    required = true,
                    description = "ID del usuario dentro del tenant",
                    example = "b9f2f1b4-0d84-4b89-9f10-1c3f0e4c7b8a"
            )
            @PathVariable UUID userId,
            @Valid
            @RequestBody(
                    required = true,
                    description = "Campos editables del usuario (no incluye roles)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AdminDtos.UserUpdateRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "email": "new.email@acme.com",
                                      "fullName": "Juan Pérez",
                                      "phone": "+54 9 11 1234-5678"
                                    }
                                    """))
            )
            AdminDtos.UserUpdateRequest req
    ) {
        IdentityPrincipal p = (IdentityPrincipal) authentication.getPrincipal();
        return admin.updateUser(p.tenantId(), p.userId(), userId, req);
    }

    @Operation(
            summary = "Listar usuarios",
            description = """
                    Devuelve los usuarios del tenant actual.
                    
                    Requiere:
                    - `identity.read` o `identity.users.manage`
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de usuarios",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AdminDtos.UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sin permiso", content = @Content)
    })
    @PreAuthorize("hasAnyAuthority(T(com.scalaris.shared.security.Permissions).IDENTITY_READ, " +
            "T(com.scalaris.shared.security.Permissions).IDENTITY_USERS_MANAGE)")
    @GetMapping("/users")
    public List<AdminDtos.UserResponse> listUsers(Authentication authentication) {
        IdentityPrincipal p = (IdentityPrincipal) authentication.getPrincipal();
        return admin.listUsers(p.tenantId(), p.userId());
    }

    @Operation(
            summary = "Obtener usuario por ID",
            description = """
                    Obtiene el detalle de un usuario del tenant actual.
                    
                    Requiere:
                    - `identity.read` o `identity.users.manage`
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AdminDtos.UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sin permiso", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content)
    })
    @PreAuthorize("hasAnyAuthority(T(com.scalaris.shared.security.Permissions).IDENTITY_READ, " +
            "T(com.scalaris.shared.security.Permissions).IDENTITY_USERS_MANAGE)")
    @GetMapping("/users/{userId}")
    public AdminDtos.UserResponse getUser(
            Authentication authentication,
            @Parameter(
                    name = "userId",
                    in = ParameterIn.PATH,
                    required = true,
                    description = "ID del usuario dentro del tenant",
                    example = "b9f2f1b4-0d84-4b89-9f10-1c3f0e4c7b8a"
            )
            @PathVariable UUID userId
    ) {
        IdentityPrincipal p = (IdentityPrincipal) authentication.getPrincipal();
        return admin.getUser(p.tenantId(), p.userId(), userId);
    }

    @Operation(
            summary = "Desactivar usuario (CUS-05)",
            description = """
                    Desactiva un usuario sin eliminar historial y revoca/invalida accesos (según tu implementación).
                    
                    Reglas clave:
                    - Requiere permiso: `identity.users.manage`.
                    - No puede desactivarse a sí mismo.
                    - No se puede desactivar al último administrador del tenant.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Desactivado (sin body)"),
            @ApiResponse(responseCode = "400", description = "Regla de negocio",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "SELF_LOCKOUT", value = """
                                            { "code": "SELF_LOCKOUT", "message": "El usuario no puede desactivarse a sí mismo." }
                                            """),
                                    @ExampleObject(name = "LAST_ADMIN", value = """
                                            { "code": "LAST_ADMIN", "message": "No se puede desactivar al último administrador del tenant" }
                                            """),
                                    @ExampleObject(name = "USER_NOT_ACTIVE", value = """
                                            { "code": "USER_NOT_ACTIVE", "message": "Solo se puede desactivar un usuario activo." }
                                            """)
                            })),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sin permiso identity.users.manage", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content)
    })
    @PreAuthorize("hasAuthority(T(com.scalaris.shared.security.Permissions).IDENTITY_USERS_MANAGE)")
    @PostMapping("/users/{userId}/deactivate")
    public void deactivateUser(
            Authentication authentication,
            @Parameter(
                    name = "userId",
                    in = ParameterIn.PATH,
                    required = true,
                    description = "ID del usuario a desactivar",
                    example = "b9f2f1b4-0d84-4b89-9f10-1c3f0e4c7b8a"
            )
            @PathVariable UUID userId
    ) {
        IdentityPrincipal p = (IdentityPrincipal) authentication.getPrincipal();
        admin.deactivateUser(p.tenantId(), p.userId(), userId);
    }

    // -------------------- ASSIGN ROLES (CUS-04) --------------------

    @Operation(
            summary = "Asignar roles a usuario (CUS-04)",
            description = """
                    Asigna/reemplaza los roles de un usuario dentro del tenant.
                    
                    Reglas clave:
                    - Requiere permiso: `identity.roles.manage`.
                    - Usuario debe estar ACTIVE.
                    - No puede dejarse un usuario sin roles.
                    - El rol debe existir y pertenecer al tenant.
                    - El cambio invalida accesos/tokens (según tu implementación).
                    - No se puede asignar un rol “superior” al propio (implementado por subset de permisos).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario con roles actualizados",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AdminDtos.UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Regla de negocio",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "USER_NOT_ACTIVE", value = """
                                            { "code": "USER_NOT_ACTIVE", "message": "El usuario debe estar activo." }
                                            """),
                                    @ExampleObject(name = "USER_ROLE_REQUIRED", value = """
                                            { "code": "USER_ROLE_REQUIRED", "message": "No se puede dejar un usuario sin rol" }
                                            """),
                                    @ExampleObject(name = "ROLE_ASSIGN_OUT_OF_BOUNDS", value = """
                                            { "code": "ROLE_ASSIGN_OUT_OF_BOUNDS", "message": "No podés asignar un rol con más permisos que los tuyos" }
                                            """),
                                    @ExampleObject(name = "LAST_ADMIN", value = """
                                            { "code": "LAST_ADMIN", "message": "No se puede desactivar al último administrador del tenant" }
                                            """)
                            })),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sin permiso identity.roles.manage", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuario/rol no encontrado", content = @Content)
    })
    @PreAuthorize("hasAuthority(T(com.scalaris.shared.security.Permissions).IDENTITY_ROLES_MANAGE)")
    @PutMapping("/users/{userId}/roles")
    public AdminDtos.UserResponse assignRoles(
            Authentication authentication,
            @Parameter(
                    name = "userId",
                    in = ParameterIn.PATH,
                    required = true,
                    description = "ID del usuario al que se le asignan roles",
                    example = "b9f2f1b4-0d84-4b89-9f10-1c3f0e4c7b8a"
            )
            @PathVariable UUID userId,
            @Valid
            @RequestBody(
                    required = true,
                    description = "Lista de roleIds que reemplaza el set actual del usuario",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AdminDtos.AssignRolesRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "roleIds": [
                                        "f4f35c7b-1d2a-4a1a-9f0c-9b3c0c7b1a11",
                                        "e2f7b2a1-9c4d-4f9f-8b1e-0d2b3c4a5f66"
                                      ]
                                    }
                                    """))
            )
            AdminDtos.AssignRolesRequest req
    ) {
        IdentityPrincipal p = (IdentityPrincipal) authentication.getPrincipal();
        return admin.assignRoles(p.tenantId(), p.userId(), userId, req);
    }

    // -------------------- ROLES --------------------

    @Operation(
            summary = "Crear rol",
            description = """
                    Crea un rol con un set de permisos dentro del tenant.
                    
                    Reglas clave:
                    - Requiere permiso: `identity.roles.manage`.
                    - El actor no puede asignar permisos que no posee (subset).
                    - Nombre de rol normalizado (según tu app service).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rol creado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AdminDtos.RoleResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "id": "f4f35c7b-1d2a-4a1a-9f0c-9b3c0c7b1a11",
                                      "name": "SALES",
                                      "permissions": ["identity.read", "crm.clients.read"]
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Regla de negocio",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "ROLE_DUPLICATE", value = """
                                            { "code": "ROLE_DUPLICATE", "message": "Ya existe un rol con ese nombre" }
                                            """),
                                    @ExampleObject(name = "ROLE_PERM_OUT_OF_BOUNDS", value = """
                                            { "code": "ROLE_PERM_OUT_OF_BOUNDS", "message": "No podés asignar permisos que no tenés (crm.clients.update)" }
                                            """)
                            })),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sin permiso identity.roles.manage", content = @Content)
    })
    @PreAuthorize("hasAuthority(T(com.scalaris.shared.security.Permissions).IDENTITY_ROLES_MANAGE)")
    @PostMapping("/roles")
    public AdminDtos.RoleResponse createRole(
            Authentication authentication,
            @Valid
            @RequestBody(
                    required = true,
                    description = "Datos de creación/edición de rol",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AdminDtos.RoleUpsertRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "name": "SALES",
                                      "permissions": ["identity.read", "crm.clients.read"]
                                    }
                                    """))
            )
            AdminDtos.RoleUpsertRequest req
    ) {
        IdentityPrincipal p = (IdentityPrincipal) authentication.getPrincipal();
        return admin.createRole(p.tenantId(), p.userId(), req);
    }

    @Operation(
            summary = "Actualizar rol",
            description = """
                    Actualiza nombre/permisos del rol.
                    
                    Reglas clave:
                    - Requiere permiso: `identity.roles.manage`.
                    - Subset: el actor no puede asignar permisos que no posee.
                    - Si el rol cambia permisos, se invalidan accesos de usuarios afectados (según tu implementación).
                    - No permitir dejar al tenant sin administradores activos (si aplicás esa regla).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rol actualizado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AdminDtos.RoleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Regla de negocio",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "ROLE_DUPLICATE", value = """
                                            { "code": "ROLE_DUPLICATE", "message": "Ya existe un rol con ese nombre" }
                                            """),
                                    @ExampleObject(name = "ROLE_PERM_OUT_OF_BOUNDS", value = """
                                            { "code": "ROLE_PERM_OUT_OF_BOUNDS", "message": "No podés asignar permisos que no tenés (identity.users.manage)" }
                                            """),
                                    @ExampleObject(name = "SELF_LOCKOUT", value = """
                                            { "code": "SELF_LOCKOUT", "message": "No podés sacarte el permiso de administrar usuarios" }
                                            """),
                                    @ExampleObject(name = "LAST_ADMIN", value = """
                                            { "code": "LAST_ADMIN", "message": "El tenant no puede quedar sin administradores" }
                                            """)
                            })),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sin permiso identity.roles.manage", content = @Content),
            @ApiResponse(responseCode = "404", description = "Rol no encontrado", content = @Content)
    })
    @PreAuthorize("hasAuthority(T(com.scalaris.shared.security.Permissions).IDENTITY_ROLES_MANAGE)")
    @PutMapping("/roles/{roleId}")
    public AdminDtos.RoleResponse updateRole(
            Authentication authentication,
            @Parameter(
                    name = "roleId",
                    in = ParameterIn.PATH,
                    required = true,
                    description = "ID del rol a actualizar",
                    example = "f4f35c7b-1d2a-4a1a-9f0c-9b3c0c7b1a11"
            )
            @PathVariable UUID roleId,
            @Valid
            @RequestBody(
                    required = true,
                    description = "Nuevo nombre y/o permisos del rol",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AdminDtos.RoleUpsertRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "name": "SALES",
                                      "permissions": ["identity.read", "crm.clients.read", "crm.clients.create"]
                                    }
                                    """))
            )
            AdminDtos.RoleUpsertRequest req
    ) {
        IdentityPrincipal p = (IdentityPrincipal) authentication.getPrincipal();
        return admin.updateRole(p.tenantId(), p.userId(), roleId, req);
    }

    @Operation(
            summary = "Listar roles",
            description = """
                    Devuelve roles del tenant actual.
                    
                    Requiere:
                    - `identity.read` o `identity.roles.manage`
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de roles",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AdminDtos.RoleResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sin permiso", content = @Content)
    })
    @PreAuthorize("hasAnyAuthority(T(com.scalaris.shared.security.Permissions).IDENTITY_READ, " +
            "T(com.scalaris.shared.security.Permissions).IDENTITY_ROLES_MANAGE)")
    @GetMapping("/roles")
    public List<AdminDtos.RoleResponse> listRoles(Authentication authentication) {
        IdentityPrincipal p = (IdentityPrincipal) authentication.getPrincipal();
        return admin.listRoles(p.tenantId());
    }

    // -------------------------------------------------------------------------
    // Nota: si querés, podés documentar permisos globales acá como referencia.
    // -------------------------------------------------------------------------
    @SuppressWarnings("unused")
    private static final class Docs {
        // Solo para documentación/IDE: asegura que estos perms existen y son visibles en navegación
        private static final String P_USERS_MANAGE = Permissions.IDENTITY_USERS_MANAGE;
        private static final String P_ROLES_MANAGE = Permissions.IDENTITY_ROLES_MANAGE;
        private static final String P_READ = Permissions.IDENTITY_READ;
    }
}
