package com.scalaris.shared.security.annotations;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize(
        "hasAnyAuthority(" +
                "T(com.scalaris.shared.security.Permissions).IDENTITY_READ, " +
                "T(com.scalaris.shared.security.Permissions).IDENTITY_USERS_MANAGE, " +
                "T(com.scalaris.shared.security.Permissions).IDENTITY_ROLES_MANAGE" +
                ")"
)
public @interface RequireIdentityRead {}
