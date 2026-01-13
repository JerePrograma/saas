package com.scalaris.auth.service;

import com.scalaris.auth.domain.User;
import com.scalaris.auth.domain.UserRole;
import com.scalaris.auth.repo.UserRepository;
import com.scalaris.auth.web.dto.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegistrationService {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public RegistrationService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public boolean isEmailAvailable(String email) {
        return !users.existsByEmailIgnoreCase(email);
    }

    public User register(RegisterRequest req) {
        if (!req.password().equals(req.confirmPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }
        if (!Boolean.TRUE.equals(req.acceptedTerms())) {
            throw new IllegalArgumentException("Debe aceptar los términos");
        }
        if (users.existsByEmailIgnoreCase(req.email())) {
            throw new EmailAlreadyRegisteredException();
        }

        String hash = encoder.encode(req.password());

        var user = new User(
                req.fullName(),
                req.email(),
                hash,
                UserRole.EMPLOYEE,          // registro público => EMPLOYEE
                req.taxPosition(),
                req.companyStructure(),
                true
        );

        return users.save(user);
    }
}
