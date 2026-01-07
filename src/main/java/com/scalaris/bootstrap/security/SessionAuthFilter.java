// ============================================================================
// bootstrap/security/SessionAuthFilter.java
// ============================================================================
package com.scalaris.bootstrap.security;

import com.scalaris.modules.identity.application.TokenUtil;
import com.scalaris.modules.identity.domain.entity.Tenant;
import com.scalaris.modules.identity.domain.entity.User;
import com.scalaris.modules.identity.domain.entity.UserSession;
import com.scalaris.modules.identity.infrastructure.jpa.TenantJpaRepository;
import com.scalaris.modules.identity.infrastructure.jpa.UserJpaRepository;
import com.scalaris.modules.identity.infrastructure.jpa.UserSessionJpaRepository;
import com.scalaris.shared.security.Permissions;
import com.scalaris.shared.tenancy.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class SessionAuthFilter extends OncePerRequestFilter {

    public static final String HEADER_TENANT = "X-Tenant-Id";
    public static final String COOKIE_NAME = "SESSION";

    private final UserSessionJpaRepository sessionRepo;
    private final UserJpaRepository userRepo;
    private final TenantJpaRepository tenantRepo;

    public SessionAuthFilter(UserSessionJpaRepository sessionRepo, UserJpaRepository userRepo, TenantJpaRepository tenantRepo) {
        this.sessionRepo = sessionRepo;
        this.userRepo = userRepo;
        this.tenantRepo = tenantRepo;
    }

    @Bean
    public SessionAuthFilter sessionAuthFilter(
            UserSessionJpaRepository sessionRepo,
            UserJpaRepository userRepo,
            TenantJpaRepository tenantRepo
    ) {
        return new SessionAuthFilter(sessionRepo, userRepo, tenantRepo);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, SessionAuthFilter sessionAuthFilter) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/login").permitAll()
                .requestMatchers("/api/v1/auth/password-reset/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated()
        );

        http.addFilterBefore(sessionAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }


    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        try {
            String rawToken = resolveToken(req);
            if (rawToken != null && !rawToken.isBlank()) {
                IdentityPrincipal principal = authenticateByToken(rawToken, req, res);

                // Si authenticateByToken decidió responder (403), no sigas.
                if (res.isCommitted()) return;

                if (principal != null) {
                    TenantContext.set(principal.tenantId());
                }
            }

            chain.doFilter(req, res);
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private IdentityPrincipal authenticateByToken(String rawToken, HttpServletRequest req, HttpServletResponse res) throws IOException {
        String hash = TokenUtil.sha256Hex(rawToken);

        UserSession s = sessionRepo.findBySessionTokenHash(hash).orElse(null);
        if (s == null) return null;

        OffsetDateTime now = OffsetDateTime.now();
        if (!s.isActiveAt(now)) return null;

        UUID tenantId = s.getTenantId();

        String path = req.getServletPath();
        boolean platformPath = path.startsWith("/api/v1/platform/");

        // Hard guard: platform endpoints solo desde platform tenant
        if (platformPath && !PlatformConstants.PLATFORM_TENANT_ID.equals(tenantId)) {
            res.sendError(403, "PLATFORM_ONLY");
            return null;
        }

        // Tenant header guard (solo para no-platform)
        if (!platformPath) {
            UUID requestedTenant = parseUuid(req.getHeader(HEADER_TENANT));
            if (requestedTenant != null && !requestedTenant.equals(tenantId)) {
                res.sendError(403, "TENANT_CONTEXT_MISMATCH");
                return null;
            }
        }

        // Tenant debe estar ACTIVE (corta sesiones si suspendés/cancelás)
        Tenant tenant = tenantRepo.findById(tenantId).orElse(null);
        if (tenant == null || tenant.getStatus() != Tenant.TenantStatus.ACTIVE) return null;

        User u = userRepo.fetchAuthGraph(tenantId, s.getUserId()).orElse(null);
        if (u == null) return null;
        if (u.getStatus() != User.UserStatus.ACTIVE) return null;

        if (!s.matchesUserStamp(u.getSecurityStamp())) return null;

        Set<SimpleGrantedAuthority> auths = buildAuthorities(u);

        IdentityPrincipal principal = new IdentityPrincipal(tenantId, u.getId(), u.getEmail());
        var token = new UsernamePasswordAuthenticationToken(principal, null, auths);
        SecurityContextHolder.getContext().setAuthentication(token);

        boolean touched = s.touchIfStale(now, u.getId(), 5);
        if (touched) sessionRepo.save(s);

        return principal;
    }

    /**
     * Construye authorities de forma robusta:
     * - Permisos canonicalizados a lower.dot (compat con hasAuthority).
     * - Filtra inválidos (si en DB quedó basura no rompe ni otorga mal).
     * - Roles normalizados a UPPER para ROLE_* (no afecta permisos).
     */
    private Set<SimpleGrantedAuthority> buildAuthorities(User u) {
        Set<SimpleGrantedAuthority> auths = new HashSet<>();

        for (var r : u.getRoles()) {
            // ROLE_* (opcional). Si no usás hasRole, igual no molesta.
            String roleName = (r.getName() == null) ? "" : r.getName().trim();
            if (!roleName.isEmpty()) {
                auths.add(new SimpleGrantedAuthority("ROLE_" + roleName.toUpperCase(Locale.ROOT)));
            }

            if (r.getPermissions() == null) continue;

            for (String rawPerm : r.getPermissions()) {
                String p = Permissions.canonicalize(rawPerm); // -> lower
                if (p == null) continue;
                if (!Permissions.isCanonical(p)) continue;    // evita wildcards / formatos raros
                auths.add(new SimpleGrantedAuthority(p));
            }
        }

        return auths;
    }

    private UUID parseUuid(String s) {
        if (s == null || s.isBlank()) return null;
        try { return UUID.fromString(s.trim()); } catch (Exception e) { return null; }
    }

    private String resolveToken(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) return auth.substring(7).trim();

        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (COOKIE_NAME.equals(c.getName())) return c.getValue();
            }
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String p = request.getServletPath();
        return p.equals("/error")
                || p.equals("/api/v1/auth/login")
                || p.startsWith("/api/v1/auth/password-reset/")
                || p.startsWith("/swagger-ui")
                || p.startsWith("/v3/api-docs");
    }

    public record IdentityPrincipal(UUID tenantId, UUID userId, String email) {}
}
