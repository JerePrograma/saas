package com.scalaris.parties.web.dto;

import com.scalaris.auth.domain.CompanyStructure;
import com.scalaris.auth.domain.TaxPosition;
import com.scalaris.parties.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record ThirdPartyCreateRequest(
        @NotNull ThirdPartyKind kind,
        @NotNull PersonType personType,
        @NotBlank String displayName,
        String legalName,
        String email,
        String phone,
        String documentType,
        String documentNumber,
        LocalDate birthDate,
        MaritalStatus maritalStatus,
        Integer childrenCount,
        Integer housesCount,
        Boolean hasPartner,
        String companyName,
        String officeName,
        Integer employeesCount,
        StylePreference stylePreference,
        TaxPosition taxPosition,
        CompanyStructure companyStructure,
        String notes,
        @Valid List<TaxIdDto> taxIds,
        @Valid List<AddressDto> addresses
) {
    public record TaxIdDto(@NotNull TaxIdType type, @NotBlank String value, boolean primary) {}
    public record AddressDto(@NotNull AddressType type, @NotBlank String line1, String line2,
                             String city, String state, String zip, String country, boolean primary) {}
}