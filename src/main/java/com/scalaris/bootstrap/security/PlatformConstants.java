package com.scalaris.bootstrap.security;

import java.util.UUID;

public final class PlatformConstants {
    private PlatformConstants() {}

    // Reservado. Usá el que prefieras, pero que sea fijo.
    public static final UUID PLATFORM_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
}
