package com.scalaris.shared.security.annotations;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasAuthority(T(com.scalaris.shared.security.Permissions).PLATFORM_TENANTS_MANAGE)")
public @interface RequirePlatformTenantsManage {}
