package com.scalaris.auth.repo;

import com.scalaris.auth.domain.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository <PasswordResetToken, UUID> {
}
