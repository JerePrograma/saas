package com.scalaris.parties.web;

import com.scalaris.api.ApiError;
import com.scalaris.parties.domain.ThirdParty;
import com.scalaris.parties.domain.ThirdPartyKind;
import com.scalaris.parties.service.ThirdPartyService;
import com.scalaris.parties.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Third Parties", description = "Gestión de clientes y proveedores (CUS-06/07/08/09).")
@RestController
@RequestMapping("/api/v1/third-parties")
@SecurityRequirement(name = "bearerAuth")
public class ThirdPartyController {

    private final ThirdPartyService service;

    public ThirdPartyController(ThirdPartyService service) {
        this.service = service;
    }

    @Operation(summary = "Crear cliente/proveedor (CUS-06)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado",
                    content = @Content(schema = @Schema(implementation = ThirdPartyResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validación/regla",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Conflicto (email/documento duplicado)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<ThirdPartyResponse> create(@RequestBody @Valid ThirdPartyCreateRequest req) {
        ThirdParty tp = service.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(tp));
    }

    @Operation(summary = "Listar clientes/proveedores (para grilla)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ThirdPartySummaryResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Request inválido",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public ResponseEntity<List<ThirdPartySummaryResponse>> list(
            @RequestParam(required = false) ThirdPartyKind kind,
            @RequestParam(required = false) String q
    ) {
        var list = service.list(kind, q).stream().map(this::toSummary).toList();
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Ver detalle (CUS-09)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(schema = @Schema(implementation = ThirdPartyResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ThirdPartyResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(service.get(id)));
    }

    @Operation(summary = "Editar ficha (CUS-07)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado",
                    content = @Content(schema = @Schema(implementation = ThirdPartyResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validación/regla",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "No existe",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Conflicto",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ThirdPartyResponse> update(@PathVariable UUID id,
                                                     @RequestBody @Valid ThirdPartyUpdateRequest req) {
        return ResponseEntity.ok(toResponse(service.update(id, req)));
    }

    @Operation(summary = "Eliminar ficha (CUS-08)", description = "Soft delete (active=false).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Eliminado"),
            @ApiResponse(responseCode = "404", description = "No existe",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // -----------------------
    // mapping (simple y directo)
    // -----------------------
    private ThirdPartySummaryResponse toSummary(ThirdParty t) {
        return new ThirdPartySummaryResponse(
                t.getId(),
                t.getKind(),
                t.getDisplayName(),
                t.getEmail(),
                t.getPhone(),
                t.getDocumentNumber(),
                t.isActive()
        );
    }

    private ThirdPartyResponse toResponse(ThirdParty t) {
        var taxIds = t.getTaxIds().stream()
                .map(x -> new ThirdPartyResponse.TaxIdDto(x.getTaxIdType(), x.getValue(), x.isPrimary()))
                .toList();

        var addresses = t.getAddresses().stream()
                .map(a -> new ThirdPartyResponse.AddressDto(
                        a.getAddressType(), a.getLine1(), a.getLine2(), a.getCity(), a.getState(),
                        a.getZip(), a.getCountry(), a.isPrimary()
                ))
                .toList();

        return new ThirdPartyResponse(
                t.getId(),
                t.getKind(),
                t.getPersonType(),
                t.getDisplayName(),
                t.getLegalName(),
                t.getEmail(),
                t.getPhone(),
                t.getDocumentType(),
                t.getDocumentNumber(),
                t.getBirthDate(),
                t.getMaritalStatus(),
                t.getChildrenCount(),
                t.getHousesCount(),
                t.getHasPartner(),
                t.getCompanyName(),
                t.getOfficeName(),
                t.getEmployeesCount(),
                t.getStylePreference(),
                t.getTaxPosition(),
                t.getCompanyStructure(),
                t.getNotes(),
                t.isActive(),
                t.getCreatedAt(),
                t.getUpdatedAt(),
                taxIds,
                addresses
        );
    }
}
