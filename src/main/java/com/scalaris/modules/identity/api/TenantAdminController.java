// ============================================================================
// modules/identity/api/TenantAdminController.java
// ============================================================================
package com.scalaris.modules.identity.api;

import com.scalaris.bootstrap.security.SessionAuthFilter.IdentityPrincipal;
import com.scalaris.modules.identity.api.dto.TenantBootstrapDtos;
import com.scalaris.modules.identity.api.dto.TenantDtos;
import com.scalaris.modules.identity.application.TenantAdminAppService;
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
        name = "Tenants (Platform)",
        description = """
                Operaciones **nivel plataforma** para CRUD de tenants y acciones administrativas.
                
                - Requiere permiso: `platform.tenants.manage`.
                - No depende de tenant del token: administra *todos* los tenants.
                - Acciones incluidas:
                  - Crear/actualizar tenant
                  - Listar/obtener tenant
                  - Cambiar status / plan
                  - Bootstrap admin inicial (solo una vez)
                """
)
@RestController
@RequestMapping("/api/v1/platform/tenants")
@SecurityRequirement(name = "bearerAuth")
public class TenantAdminController {

    private final TenantAdminAppService tenants;

    public TenantAdminController(TenantAdminAppService tenants) {
        this.tenants = tenants;
    }

    @Operation(
            summary = "Crear tenant",
            description = """
                    Crea un tenant nuevo en plataforma.
                    
                    Reglas típicas (según tu AppService):
                    - Nombre único (case-insensitive) a nivel plataforma.
                    - Plan: BASIC | PRO | ENTERPRISE.
                    - Genera un **slug estable** (tenant key tipo SLUG) para login/identificación externa.
                    - Si settings viene null, se persiste `{}`.
                    - Se seed-ean entitlements por plan.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tenant creado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TenantDtos.TenantResponse.class),
                            examples = @ExampleObject(name = "TenantResponse", value = """
                                    {
                                      "id": "1c8dbd77-0f6d-41d0-9f40-5c3f6ab2c210",
                                      "name": "Mueblería Juan",
                                      "status": "ACTIVE",
                                      "plan": "BASIC",
                                      "slug": "muebleria-juan",
                                      "settings": { "modules": { "inventory": true } }
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Validación / regla de negocio",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "TENANT_DUPLICATE_NAME", value = """
                                            { "code": "TENANT_DUPLICATE_NAME", "message": "Ya existe un tenant con ese nombre" }
                                            """),
                                    @ExampleObject(name = "TENANT_INVALID_PLAN", value = """
                                            { "code": "TENANT_INVALID_PLAN", "message": "Plan inválido: GOLD" }
                                            """)
                            })),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sin permiso platform.tenants.manage", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            { "code": "FORBIDDEN", "message": "Falta permiso: platform.tenants.manage" }
                            """)
            ))
    })
    @PreAuthorize("hasAuthority(T(com.scalaris.shared.security.Permissions).PLATFORM_TENANTS_MANAGE)")
    @PostMapping
    public TenantDtos.TenantResponse create(
            Authentication auth,
            @Valid
            @RequestBody(
                    required = true,
                    description = "Datos de creación del tenant",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TenantDtos.TenantCreateRequest.class),
                            examples = {
                                    @ExampleObject(name = "BASIC", value = """
                                            {
                                              "name": "Mueblería Juan",
                                              "plan": "BASIC",
                                              "settings": { "modules": { "inventory": true } }
                                            }
                                            """),
                                    @ExampleObject(name = "PRO", value = """
                                            {
                                              "name": "Acme Corp",
                                              "plan": "PRO",
                                              "settings": { "limits": { "users": 25 } }
                                            }
                                            """)
                            })
            )
            TenantDtos.TenantCreateRequest req
    ) {
        UUID actorId = principal(auth).userId();
        return tenants.create(actorId, req);
    }

    @Operation(
            summary = "Bootstrap admin del tenant",
            description = """
                    Inicializa el tenant creando:
                    - Rol `ADMIN` con permisos base Identity (login/read/users.manage/roles.manage)
                    - Usuario owner asociado al rol
                    
                    Restricción:
                    - **Solo puede ejecutarse 1 vez** por tenant.
                    - Si el tenant ya tiene usuarios, falla.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bootstrap completado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TenantBootstrapDtos.BootstrapAdminResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "tenantId": "1c8dbd77-0f6d-41d0-9f40-5c3f6ab2c210",
                                      "ownerUserId": "b9f2f1b4-0d84-4b89-9f10-1c3f0e4c7b8a",
                                      "adminRoleId": "f4f35c7b-1d2a-4a1a-9f0c-9b3c0c7b1a11",
                                      "permissions": [
                                        "identity.login",
                                        "identity.read",
                                        "identity.users.manage",
                                        "identity.roles.manage"
                                      ]
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Regla de negocio",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "TENANT_ALREADY_BOOTSTRAPPED", value = """
                                            { "code": "TENANT_ALREADY_BOOTSTRAPPED", "message": "El tenant ya tiene usuarios" }
                                            """)
                            })),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sin permiso platform.tenants.manage", content = @Content),
            @ApiResponse(responseCode = "404", description = "Tenant no encontrado", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            { "code": "NOT_FOUND", "message": "Tenant no encontrado" }
                            """)
            ))
    })
    @PreAuthorize("hasAuthority(T(com.scalaris.shared.security.Permissions).PLATFORM_TENANTS_MANAGE)")
    @PostMapping("/{tenantId}/bootstrap-admin")
    public TenantBootstrapDtos.BootstrapAdminResponse bootstrapAdmin(
            Authentication auth,
            @Parameter(
                    name = "tenantId",
                    in = ParameterIn.PATH,
                    required = true,
                    description = "ID del tenant a bootstrapping (nivel plataforma)",
                    example = "1c8dbd77-0f6d-41d0-9f40-5c3f6ab2c210"
            )
            @PathVariable UUID tenantId,
            @Valid
            @RequestBody(
                    required = true,
                    description = "Credenciales del owner inicial del tenant",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TenantBootstrapDtos.BootstrapAdminRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "email": "owner@acme.com",
                                      "password": "Str0ngPass!",
                                      "fullName": "Owner Admin"
                                    }
                                    """))
            )
            TenantBootstrapDtos.BootstrapAdminRequest req
    ) {
        UUID actorId = principal(auth).userId();
        return tenants.bootstrapAdmin(actorId, tenantId, req);
    }

    @Operation(
            summary = "Actualizar tenant",
            description = """
                    Actualiza el nombre y/o settings del tenant.
                    
                    Notas:
                    - No cambia slug automáticamente (para no romper URLs).
                    - Si settings viene null, se persiste `{}`.
                    - Nombre debe seguir siendo único (case-insensitive) a nivel plataforma.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tenant actualizado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TenantDtos.TenantResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validación / regla de negocio",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "TENANT_DUPLICATE_NAME", value = """
                                            { "code": "TENANT_DUPLICATE_NAME", "message": "Ya existe un tenant con ese nombre" }
                                            """)
                            })),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sin permiso platform.tenants.manage", content = @Content),
            @ApiResponse(responseCode = "404", description = "Tenant no encontrado", content = @Content)
    })
    @PreAuthorize("hasAuthority(T(com.scalaris.shared.security.Permissions).PLATFORM_TENANTS_MANAGE)")
    @PutMapping("/{tenantId}")
    public TenantDtos.TenantResponse update(
            Authentication auth,
            @Parameter(
                    name = "tenantId",
                    in = ParameterIn.PATH,
                    required = true,
                    description = "ID del tenant a actualizar",
                    example = "1c8dbd77-0f6d-41d0-9f40-5c3f6ab2c210"
            )
            @PathVariable UUID tenantId,
            @Valid
            @RequestBody(
                    required = true,
                    description = "Cambios del tenant",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TenantDtos.TenantUpdateRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "name": "Mueblería Juan (Sucursal Centro)",
                                      "settings": { "limits": { "users": 10 } }
                                    }
                                    """))
            )
            TenantDtos.TenantUpdateRequest req
    ) {
        UUID actorId = principal(auth).userId();
        return tenants.update(actorId, tenantId, req);
    }

    @Operation(
            summary = "Obtener tenant",
            description = "Obtiene un tenant por ID (nivel plataforma)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tenant",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TenantDtos.TenantResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sin permiso platform.tenants.manage", content = @Content),
            @ApiResponse(responseCode = "404", description = "Tenant no encontrado", content = @Content)
    })
    @PreAuthorize("hasAuthority(T(com.scalaris.shared.security.Permissions).PLATFORM_TENANTS_MANAGE)")
    @GetMapping("/{tenantId}")
    public TenantDtos.TenantResponse get(
            @Parameter(
                    name = "tenantId",
                    in = ParameterIn.PATH,
                    required = true,
                    description = "ID del tenant a consultar",
                    example = "1c8dbd77-0f6d-41d0-9f40-5c3f6ab2c210"
            )
            @PathVariable UUID tenantId
    ) {
        return tenants.get(tenantId);
    }

    @Operation(
            summary = "Listar tenants",
            description = "Lista tenants de la plataforma. (Si necesitás paginado/filtros, este endpoint es candidato.)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de tenants",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TenantDtos.TenantResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sin permiso platform.tenants.manage", content = @Content)
    })
    @PreAuthorize("hasAuthority(T(com.scalaris.shared.security.Permissions).PLATFORM_TENANTS_MANAGE)")
    @GetMapping
    public List<TenantDtos.TenantResponse> list() {
        return tenants.list();
    }

    @Operation(
            summary = "Cambiar status del tenant",
            description = """
                    Cambia el status del tenant.
                    
                    Valores permitidos:
                    - ACTIVE
                    - SUSPENDED
                    - CANCELED
                    
                    Nota: es una operación administrativa (command).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status actualizado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TenantDtos.TenantResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validación / regla de negocio",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "code": "TENANT_INVALID_STATUS", "message": "Status inválido: PAUSED" }
                                    """))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sin permiso platform.tenants.manage", content = @Content),
            @ApiResponse(responseCode = "404", description = "Tenant no encontrado", content = @Content)
    })
    @PreAuthorize("hasAuthority(T(com.scalaris.shared.security.Permissions).PLATFORM_TENANTS_MANAGE)")
    @PostMapping("/{tenantId}/status")
    public TenantDtos.TenantResponse changeStatus(
            Authentication auth,
            @Parameter(
                    name = "tenantId",
                    in = ParameterIn.PATH,
                    required = true,
                    description = "ID del tenant a modificar",
                    example = "1c8dbd77-0f6d-41d0-9f40-5c3f6ab2c210"
            )
            @PathVariable UUID tenantId,
            @Valid
            @RequestBody(
                    required = true,
                    description = "Nuevo status del tenant",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TenantDtos.TenantChangeStatusRequest.class),
                            examples = @ExampleObject(value = """
                                    { "status": "SUSPENDED" }
                                    """))
            )
            TenantDtos.TenantChangeStatusRequest req
    ) {
        UUID actorId = principal(auth).userId();
        return tenants.changeStatus(actorId, tenantId, req);
    }

    @Operation(
            summary = "Cambiar plan del tenant",
            description = """
                    Cambia el plan del tenant.
                    
                    Valores permitidos:
                    - BASIC
                    - PRO
                    - ENTERPRISE
                    
                    Nota: al cambiar plan, el sistema puede seedear/ajustar entitlements por defecto.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plan actualizado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TenantDtos.TenantResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validación / regla de negocio",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "code": "TENANT_INVALID_PLAN", "message": "Plan inválido: GOLD" }
                                    """))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sin permiso platform.tenants.manage", content = @Content),
            @ApiResponse(responseCode = "404", description = "Tenant no encontrado", content = @Content)
    })
    @PreAuthorize("hasAuthority(T(com.scalaris.shared.security.Permissions).PLATFORM_TENANTS_MANAGE)")
    @PostMapping("/{tenantId}/plan")
    public TenantDtos.TenantResponse changePlan(
            Authentication auth,
            @Parameter(
                    name = "tenantId",
                    in = ParameterIn.PATH,
                    required = true,
                    description = "ID del tenant a modificar",
                    example = "1c8dbd77-0f6d-41d0-9f40-5c3f6ab2c210"
            )
            @PathVariable UUID tenantId,
            @Valid
            @RequestBody(
                    required = true,
                    description = "Nuevo plan del tenant",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TenantDtos.TenantChangePlanRequest.class),
                            examples = @ExampleObject(value = """
                                    { "plan": "PRO" }
                                    """))
            )
            TenantDtos.TenantChangePlanRequest req
    ) {
        UUID actorId = principal(auth).userId();
        return tenants.changePlan(actorId, tenantId, req);
    }

    private static IdentityPrincipal principal(Authentication auth) {
        return (IdentityPrincipal) auth.getPrincipal();
    }

    @SuppressWarnings("unused")
    private static final class Docs {
        private static final String P_PLATFORM_TENANTS = Permissions.PLATFORM_TENANTS_MANAGE;
    }
}
