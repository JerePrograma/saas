// ============================================================================
// shared/tracing/TraceIdFilter.java
// ============================================================================
package com.scalaris.shared.tracing;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) {
        try {
            String incoming = req.getHeader(TraceIds.HEADER);
            TraceIds.set(incoming);
            String traceId = TraceIds.getOrCreate();
            res.setHeader(TraceIds.HEADER, traceId);
            chain.doFilter(req, res);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            TraceIds.clear();
        }
    }
}
