package com.scalaris.shared.tenancy;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class TenantFilter implements Filter {

    @Value("${app.tenancy.header:X-Tenant-Id}")
    private String header;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String raw = req.getHeader(header);

        try {
            if (raw != null && !raw.isBlank()) {
                TenantContext.set(UUID.fromString(raw.trim()));
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
