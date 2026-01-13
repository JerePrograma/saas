package com.scalaris.auth.web.dto;

import com.scalaris.auth.domain.CompanyStructure;
import com.scalaris.auth.domain.TaxPosition;
import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 40) String fullName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 12) String password,
        @NotBlank String confirmPassword,
        @NotNull Boolean acceptedTerms,
        TaxPosition taxPosition,
        CompanyStructure companyStructure
) {}
