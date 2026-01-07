// ============================================================================
// shared/security/CurrentActor.java
// ============================================================================
package com.scalaris.shared.security;

import java.util.UUID;

public interface CurrentActor {
    UUID userId();
    boolean has(String permission);
}
