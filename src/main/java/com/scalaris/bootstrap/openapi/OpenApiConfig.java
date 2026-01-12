package com.scalaris.bootstrap.openapi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Scalaris API",
                version = "v1",
                description = """
                ### Quickstart (Flow 0)

                **A) Tenant (usuario normal)**
                1) `POST /api/v1/auth/login` con header **X-Tenant-Key** (slug) + credenciales en el body.
                   - Devuelve `sessionToken` y además setea cookie **SESSION** (si estás en browser/mismo dominio).
                2) Elegí un método de auth para el resto:
                   - **Bearer**: `Authorization: Bearer <sessionToken>`
                   - **Cookie**: el browser manda `SESSION` automáticamente
                3) Validar sesión: `GET /api/v1/auth/me`
                4) Gestión del tenant: `/api/v1/identity/**` (**NO** usa header de tenant; el tenant sale del token/cookie)

                **B) Platform (admin plataforma)**
                - CRUD tenants: `/api/v1/platform/tenants/**` (requiere `platform.tenants.manage`)
                """
        )
)
@io.swagger.v3.oas.annotations.tags.Tag(
        name = "01 - Auth (Public)",
        description = "Flow 0 tenant: login/reset/me/logout. Requiere X-Tenant-Key SOLO en login/reset."
)
@io.swagger.v3.oas.annotations.tags.Tag(
        name = "02 - Identity - Users",
        description = "Usuarios del tenant. Tenant deriva del token/cookie. Requiere permisos identity.*."
)
@io.swagger.v3.oas.annotations.tags.Tag(
        name = "02 - Identity - Roles",
        description = "Roles del tenant. Tenant deriva del token/cookie. Requiere permisos identity.*."
)
@io.swagger.v3.oas.annotations.tags.Tag(
        name = "03 - Platform - Tenants",
        description = "Operaciones plataforma para tenants. No depende del tenant del token. Requiere platform.tenants.manage."
)
@SecurityScheme(
        name = "tenantKey",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-Tenant-Key",
        description = "Tenant key/slug (ej: demo). Solo para endpoints públicos (login/reset)."
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "opaque"
)
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi authPublicApi() {
        return GroupedOpenApi.builder()
                .group("01 - Auth (Public)")
                .pathsToMatch("/api/v1/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi identityTenantApi() {
        return GroupedOpenApi.builder()
                .group("02 - Identity (Tenant)")
                .pathsToMatch("/api/v1/identity/**")
                .build();
    }

    @Bean
    public GroupedOpenApi platformApi() {
        return GroupedOpenApi.builder()
                .group("03 - Platform")
                .pathsToMatch("/api/v1/platform/**")
                .build();
    }
}
