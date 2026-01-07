// ============================================================================
// shared/api/PageResponse.java
// ============================================================================
package com.scalaris.shared.api;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalItems
) { }
