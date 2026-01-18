package com.scalaris.parties.web.dto;


import com.scalaris.parties.domain.ThirdPartyKind;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "ThirdPartySummaryResponse")
public record ThirdPartySummaryResponse(
        UUID id,
        ThirdPartyKind kind,
        String displayName,
        String email,
        String phone,
        String documentNumber,
        boolean active
) {}
