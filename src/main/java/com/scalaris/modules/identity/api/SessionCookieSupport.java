package com.scalaris.modules.identity.api;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class SessionCookieSupport {

    public static final String COOKIE_NAME = "SESSION";

    private final boolean secure;
    private final String sameSite;

    public SessionCookieSupport(
            @Value("${app.cookies.secure:false}") boolean secure,
            @Value("${app.cookies.same-site:Lax}") String sameSite
    ) {
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public void setSession(HttpServletResponse res, String token, boolean rememberMe) {
        long maxAge = rememberMe ? 60L * 60 * 24 * 30 : 60L * 60 * 8;

        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secure)          // true en prod con https
                .sameSite(sameSite)      // Lax por defecto (razonable)
                .path("/")
                .maxAge(maxAge)
                .build();

        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearSession(HttpServletResponse res) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(0)
                .build();

        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
