package com.scalaris.parties.web.dto;

import com.scalaris.auth.domain.CompanyStructure;
import com.scalaris.auth.domain.TaxPosition;
import com.scalaris.parties.domain.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Schema(name = "ThirdPartyResponse")
public record ThirdPartyResponse(
        UUID id,
        ThirdPartyKind kind,
        PersonType personType,
        String displayName,
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
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<TaxIdDto> taxIds,
        List<AddressDto> addresses
) {
    public record TaxIdDto(TaxIdType type, String value, boolean primary) {}
    public record AddressDto(AddressType type, String line1, String line2, String city, String state,
                             String zip, String country, boolean primary) {}
}