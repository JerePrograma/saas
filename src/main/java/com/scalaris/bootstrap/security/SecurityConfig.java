package com.scalaris.bootstrap.security;

import com.scalaris.modules.identity.infrastructure.jpa.TenantJpaRepository;
import com.scalaris.modules.identity.infrastructure.jpa.UserJpaRepository;
import com.scalaris.modules.identity.infrastructure.jpa.UserSessionJpaRepository;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            UserSessionJpaRepository sessionRepo,
            UserJpaRepository userRepo,
            TenantJpaRepository tenantRepo
    ) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable);

        http.sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/login").permitAll()
                .requestMatchers("/api/v1/auth/password-reset/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated()
        );

        http.addFilterBefore(
                new SessionAuthFilter(sessionRepo, userRepo, tenantRepo),
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }
}
