package com.scalaris.auth.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.scalaris.auth.domain.User;
import com.scalaris.config.JwtProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class JwtService {

    private final Algorithm algorithm;
    private final JwtProperties props;

    public JwtService(JwtProperties props) {
        this.props = props;
        this.algorithm = Algorithm.HMAC256(props.getSecret());
    }

    public String createAccessToken(User user, UUID jti) {
        Instant exp = OffsetDateTime.now().plusSeconds(props.getAccessTtlSeconds()).toInstant();
        return JWT.create()
                .withIssuer(props.getIssuer())
                .withSubject(user.getEmail())
                .withJWTId(jti.toString())
                .withClaim("uid", user.getId().toString())
                .withClaim("role", user.getRole().name())
                .withClaim("type", "ACCESS")
                .withExpiresAt(exp)
                .sign(algorithm);
    }

    public String createRefreshToken(User user, UUID jti) {
        Instant exp = OffsetDateTime.now().plusSeconds(props.getRefreshTtlSeconds()).toInstant();
        return JWT.create()
                .withIssuer(props.getIssuer())
                .withSubject(user.getEmail())
                .withJWTId(jti.toString())
                .withClaim("uid", user.getId().toString())
                .withClaim("type", "REFRESH")
                .withExpiresAt(exp)
                .sign(algorithm);
    }

    public DecodedJWT verify(String token) {
        return JWT.require(algorithm)
                .withIssuer(props.getIssuer())
                .build()
                .verify(token);
    }
}
