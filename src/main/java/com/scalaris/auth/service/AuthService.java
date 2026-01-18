package com.scalaris.auth.service;

import com.scalaris.auth.repo.UserRepository;
import com.scalaris.auth.web.dto.LoginRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AuthService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public com.scalaris.auth.domain.User authenticate(LoginRequest req) {
        var user = users.findByEmailIgnoreCase(req.email().trim())
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isActive()) throw new InvalidCredentialsException();
        if (!encoder.matches(req.password(), user.getPasswordHash())) throw new InvalidCredentialsException();

        return user;
    }
}
