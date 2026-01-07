package com.scalaris.shared.tenancy;

import java.util.UUID;

public final class TenantContext {
    public static final String HEADER = "X-Tenant-Id";

    private static final ThreadLocal<UUID> TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(UUID tenantId) { TENANT.set(tenantId); }
    public static UUID get() { return TENANT.get(); }

    public static UUID getRequired() {
        UUID t = TENANT.get();
        if (t == null) throw new IllegalStateException("TenantContext no seteado");
        return t;
    }

    public static void clear() { TENANT.remove(); }
}
