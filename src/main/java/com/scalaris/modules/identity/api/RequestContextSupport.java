package com.scalaris.modules.identity.api;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestContextSupport {
    private RequestContextSupport() {}

    public static String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();

        String xri = req.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) return xri.trim();

        return req.getRemoteAddr();
    }
}
