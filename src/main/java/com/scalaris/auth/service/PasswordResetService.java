package com.scalaris.auth.service;

import com.scalaris.auth.domain.PasswordResetToken;
import com.scalaris.auth.repo.PasswordResetTokenRepository;
import com.scalaris.auth.repo.UserRepository;
import com.scalaris.auth.web.dto.PasswordResetConfirmRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class PasswordResetService {

    public interface MailSender {
        void send(String to, String subject, String body);
    }

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final MailSender mail;

    public PasswordResetService(UserRepository users,
                                PasswordResetTokenRepository tokens,
                                PasswordEncoder encoder,
                                MailSender mail) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
        this.mail = mail;
    }

    @Transactional
    public void request(String email) {
        // Respuesta genérica para no filtrar si existe o no.
        var userOpt = users.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) return;

        var user = userOpt.get();

        UUID resetId = UUID.randomUUID();
        String code = random6Digits();
        String codeHash = encoder.encode(code);
        var expires = OffsetDateTime.now().plusHours(24);

        tokens.save(new PasswordResetToken(resetId, user.getId(), codeHash, expires));

        mail.send(user.getEmail(),
                "Recuperación de contraseña",
                "Tu código: " + code + "\nID: " + resetId);
    }

    @Transactional
    public void confirm(PasswordResetConfirmRequest req) {
        if (!req.newPassword().equals(req.confirmPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }

        var prt = tokens.findById(req.resetId())
                .orElseThrow(() -> new IllegalArgumentException("Código inválido o caducado"));

        if (prt.isUsed() || prt.isExpired()) {
            throw new IllegalArgumentException("Código inválido o caducado");
        }
        if (!encoder.matches(req.code(), prt.getCodeHash())) {
            throw new IllegalArgumentException("Código inválido o caducado");
        }

        var user = users.findById(prt.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Código inválido o caducado"));

        user.setFullName(user.getFullName()); // no-op, solo para mostrar que usamos entidad
        // actualizar password
        // (tu User no tiene setter, lo cual es correcto; agregá setPasswordHash)
        // user.setPasswordHash(encoder.encode(req.newPassword()));

        prt.markUsed();
        tokens.save(prt);
        users.save(user);
    }

    private String random6Digits() {
        int n = (int)(Math.random() * 900000) + 100000;
        return Integer.toString(n);
    }
}
