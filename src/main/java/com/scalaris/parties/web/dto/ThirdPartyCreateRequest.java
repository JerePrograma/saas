package com.scalaris.parties.web.dto;

import com.scalaris.auth.domain.CompanyStructure;
import com.scalaris.auth.domain.TaxPosition;
import com.scalaris.parties.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

public record ThirdPartyCreateRequest(
        @NotNull ThirdPartyKind kind,
        @NotNull PersonType personType,

        @NotBlank
        @Size(min = 3, max = 40, message = "Debe tener entre 3 y 40 caracteres")
        String displayName,

        @Size(max = 160) String legalName,

        @Email @Size(max = 254) String email,
        @Size(max = 40) String phone,

        @Size(max = 20) String documentType,
        @Size(max = 40) String documentNumber,

        @PastOrPresent LocalDate birthDate,

        MaritalStatus maritalStatus,
        @Min(0) Integer childrenCount,
        @Min(0) Integer housesCount,
        Boolean hasPartner,

        @Size(max = 160) String companyName,
        @Size(max = 160) String officeName,
        @Min(0) Integer employeesCount,

        StylePreference stylePreference,
        TaxPosition taxPosition,
        CompanyStructure companyStructure,

        String notes,

        @Valid List<TaxIdDto> taxIds,
        @Valid List<AddressDto> addresses
) {
    public record TaxIdDto(
            @NotNull TaxIdType type,
            @NotBlank @Size(max = 40) String value,
            boolean primary
    ) {}

    public record AddressDto(
            @NotNull AddressType type,
            @NotBlank @Size(max = 200) String line1,
            @Size(max = 200) String line2,
            @Size(max = 80) String city,
            @Size(max = 80) String state,
            @Size(max = 20) String zip,
            @Size(max = 80) String country,
            boolean primary
    ) {}
}
