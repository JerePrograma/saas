package com.scalaris.parties.web.dto;


import com.scalaris.auth.domain.CompanyStructure;
import com.scalaris.auth.domain.TaxPosition;
import com.scalaris.parties.domain.MaritalStatus;
import com.scalaris.parties.domain.PersonType;
import com.scalaris.parties.domain.StylePreference;
import com.scalaris.parties.domain.ThirdPartyKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record ThirdPartyUpdateRequest(
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
        List<ThirdPartyCreateRequest.TaxIdDto> taxIds,
        List<ThirdPartyCreateRequest.AddressDto> addresses
) {}
