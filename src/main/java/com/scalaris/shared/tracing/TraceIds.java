// ============================================================================
// shared/tracing/TraceIds.java
// ============================================================================
package com.scalaris.shared.tracing;

import org.slf4j.MDC;

import java.util.UUID;

public final class TraceIds {
    public static final String MDC_KEY = "traceId";
    public static final String HEADER = "X-Trace-Id";

    private TraceIds() {}

    public static String getOrCreate() {
        String v = MDC.get(MDC_KEY);
        if (v == null || v.isBlank()) {
            v = UUID.randomUUID().toString();
            MDC.put(MDC_KEY, v);
        }
        return v;
    }

    public static String get() {
        return MDC.get(MDC_KEY);
    }

    public static void set(String traceId) {
        if (traceId != null && !traceId.isBlank()) MDC.put(MDC_KEY, traceId.trim());
    }

    public static void clear() {
        MDC.remove(MDC_KEY);
    }
}
