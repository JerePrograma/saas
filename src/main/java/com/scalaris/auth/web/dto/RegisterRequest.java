package com.scalaris.auth.web.dto;

import com.scalaris.auth.domain.CompanyStructure;
import com.scalaris.auth.domain.TaxPosition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(name = "RegisterRequest", description = "Registro de usuario (Step1 + Step2)")
public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 40)
        @Schema(example = "Juan Pérez")
        String fullName,

        @NotBlank @Email
        @Schema(example = "juan@demo.com")
        String email,

        @NotBlank @Size(min = 8, max = 12)
        @Schema(example = "Abcdef12")
        String password,

        @NotBlank
        @Schema(example = "Abcdef12")
        String confirmPassword,

        @NotNull
        @Schema(example = "true")
        Boolean acceptedTerms,

        @Schema(description = "Posición fiscal", example = "MONOTRIBUTO")
        TaxPosition taxPosition,

        @Schema(description = "Estructura empresarial", example = "UNIPERSONAL")
        CompanyStructure companyStructure
) {}
