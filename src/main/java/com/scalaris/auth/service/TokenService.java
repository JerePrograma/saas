package com.scalaris.auth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.scalaris.auth.domain.RefreshToken;
import com.scalaris.auth.domain.User;
import com.scalaris.auth.repo.RefreshTokenRepository;
import com.scalaris.config.JwtProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;

@Service
public class TokenService {

    private final JwtProperties props;
    private final RefreshTokenRepository refreshRepo;

    public TokenService(JwtProperties props, RefreshTokenRepository refreshRepo) {
        this.props = props;
        this.refreshRepo = refreshRepo;
    }

    public String generateAccessToken(User user) {
        return generateJwt(user, props.getAccessTtlSeconds(), "ACCESS", null);
    }

    @Transactional
    public String generateAndStoreRefreshToken(User user) {
        UUID jti = UUID.randomUUID();
        String jwt = generateJwt(user, props.getRefreshTtlSeconds(), "REFRESH", jti);

        OffsetDateTime expiresAt = OffsetDateTime.now().plusSeconds(props.getRefreshTtlSeconds());
        refreshRepo.save(new RefreshToken(jti, user.getId(), expiresAt));

        return jwt;
    }

    @Transactional
    public IssuedTokens issueTokens(User user) {
        String access = generateAccessToken(user);
        String refresh = generateAndStoreRefreshToken(user);
        return new IssuedTokens(access, props.getAccessTtlSeconds(), refresh, props.getRefreshTtlSeconds());
    }

    @Transactional
    public IssuedTokens refresh(com.auth0.jwt.interfaces.DecodedJWT decoded, User user) {
        if (!"REFRESH".equals(decoded.getClaim("type").asString())) {
            throw new com.auth0.jwt.exceptions.JWTVerificationException("Token no es REFRESH");
        }
        if (decoded.getId() == null) {
            throw new com.auth0.jwt.exceptions.JWTVerificationException("Refresh sin jti");
        }

        java.util.UUID jti = java.util.UUID.fromString(decoded.getId());
        java.util.UUID subUserId = java.util.UUID.fromString(decoded.getSubject());

        if (!subUserId.equals(user.getId())) {
            throw new com.auth0.jwt.exceptions.JWTVerificationException("Refresh no pertenece al usuario");
        }

        var row = refreshRepo.findByIdAndRevokedAtIsNull(jti)
                .orElseThrow(() -> new com.auth0.jwt.exceptions.JWTVerificationException("Refresh revocado o inexistente"));

        if (row.isExpired()) throw new com.auth0.jwt.exceptions.JWTVerificationException("Refresh expirado");

        row.revokeNow();
        refreshRepo.save(row);

        String newAccess = generateAccessToken(user);
        String newRefresh = generateAndStoreRefreshToken(user);

        return new IssuedTokens(newAccess, props.getAccessTtlSeconds(), newRefresh, props.getRefreshTtlSeconds());
    }

    @Transactional
    public void revokeRefresh(String refreshJwt) {
        DecodedJWT decoded = verify(refreshJwt);

        if (!"REFRESH".equals(decoded.getClaim("type").asString())) {
            throw new JWTVerificationException("Token no es REFRESH");
        }
        if (decoded.getId() == null) throw new JWTVerificationException("Refresh sin jti");

        UUID jti = UUID.fromString(decoded.getId());

        RefreshToken row = refreshRepo.findByIdAndRevokedAtIsNull(jti)
                .orElseThrow(() -> new JWTVerificationException("Refresh inválido"));

        row.revokeNow();
        refreshRepo.save(row);
    }

    public DecodedJWT verify(String token) {
        Algorithm alg = Algorithm.HMAC256(props.getSecret());
        return JWT.require(alg).withIssuer(props.getIssuer()).build().verify(token);
    }

    private String generateJwt(User user, long ttlSeconds, String type, UUID jti) {
        Algorithm alg = Algorithm.HMAC256(props.getSecret());
        Date exp = new Date(System.currentTimeMillis() + ttlSeconds * 1000);

        var builder = JWT.create()
                .withIssuer(props.getIssuer())
                .withSubject(user.getId().toString()) // sub = userId
                .withClaim("type", type)
                .withClaim("email", user.getEmail())
                .withClaim("role", user.getRole().name())
                .withExpiresAt(exp);

        if (jti != null) builder.withJWTId(jti.toString());

        return builder.sign(alg);
    }

    public record IssuedTokens(
            String accessToken,
            long accessExpiresInSeconds,
            String refreshToken,
            long refreshExpiresInSeconds
    ) {}
}
