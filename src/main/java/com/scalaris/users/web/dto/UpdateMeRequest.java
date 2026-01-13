package com.scalaris.users.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateMeRequest(
        @Size(min=3, max=40) String fullName,
        @Email String email,
        String currentPassword,
        @Size(min=8, max=12) String newPassword,
        String confirmNewPassword
) {}