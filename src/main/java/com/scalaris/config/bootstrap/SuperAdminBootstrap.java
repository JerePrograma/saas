package com.scalaris.config.bootstrap;

import com.scalaris.auth.domain.User;
import com.scalaris.auth.domain.UserRole;
import com.scalaris.auth.repo.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SuperAdminBootstrap implements ApplicationRunner {

    private final UserRepository users;
    private final Environment env;
    private final PasswordEncoder encoder;

    public SuperAdminBootstrap(UserRepository users, Environment env, PasswordEncoder encoder) {
        this.users = users;
        this.env = env;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = env.getProperty("SCALARIS_SUPERADMIN_EMAIL");
        String pass  = env.getProperty("SCALARIS_SUPERADMIN_PASSWORD");
        if (email == null || pass == null) return;

        // Criterio: si no existe ningún SUPERADMIN, lo creo.
        if (users.existsByRole(UserRole.ADMIN)) return;

        String normalizedEmail = email.trim().toLowerCase();
        if (users.existsByEmail(normalizedEmail)) return; // evita colisión

        User u = new User(
                "Super Admin",
                normalizedEmail,
                encoder.encode(pass),
                UserRole.ADMIN,
                null,
                null,
                true
        );
        users.save(u);
    }
}
