package com.scalaris.parties.service;

import com.scalaris.parties.domain.*;
import com.scalaris.parties.repo.ThirdPartyRepository;
import com.scalaris.parties.web.dto.*;
import com.scalaris.shared.errors.ConflictException;
import com.scalaris.shared.errors.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ThirdPartyService {

    private final ThirdPartyRepository repo;

    public ThirdPartyService(ThirdPartyRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public ThirdParty create(ThirdPartyCreateRequest req) {
        var tp = new ThirdParty();

        applyCore(tp, req.kind(), req.personType(), req.displayName(), req.legalName(), req.email(), req.phone(),
                req.documentType(), req.documentNumber(), req.birthDate(), req.maritalStatus(),
                req.childrenCount(), req.housesCount(), req.hasPartner(),
                req.companyName(), req.officeName(), req.employeesCount(),
                req.stylePreference(), req.taxPosition(), req.companyStructure(), req.notes()
        );

        enforceUniqueness(tp, null);

        syncTaxIds(tp, req.taxIds());
        syncAddresses(tp, req.addresses());

        enforcePrimaryRules(tp);

        return repo.save(tp);
    }

    @Transactional
    public ThirdParty update(UUID id, ThirdPartyUpdateRequest req) {
        var tp = repo.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Tercero inexistente"));

        applyCore(tp, req.kind(), req.personType(), req.displayName(), req.legalName(), req.email(), req.phone(),
                req.documentType(), req.documentNumber(), req.birthDate(), req.maritalStatus(),
                req.childrenCount(), req.housesCount(), req.hasPartner(),
                req.companyName(), req.officeName(), req.employeesCount(),
                req.stylePreference(), req.taxPosition(), req.companyStructure(), req.notes()
        );

        enforceUniqueness(tp, id);

        syncTaxIds(tp, req.taxIds());
        syncAddresses(tp, req.addresses());

        enforcePrimaryRules(tp);

        return repo.save(tp);
    }

    @Transactional(readOnly = true)
    public ThirdParty get(UUID id) {
        return repo.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Tercero inexistente"));
    }

    @Transactional(readOnly = true)
    public List<ThirdParty> list(ThirdPartyKind kind, String q) {
        String qq = (q == null || q.isBlank()) ? null : q.trim();
        return repo.searchActive(kind, qq);
    }

    @Transactional
    public void delete(UUID id) {
        var tp = repo.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Tercero inexistente"));
        tp.deactivate(); // soft delete
        repo.save(tp);
    }

    // -----------------------------
    // Internals
    // -----------------------------
    private void applyCore(
            ThirdParty tp,
            ThirdPartyKind kind,
            PersonType personType,
            String displayName,
            String legalName,
            String email,
            String phone,
            String documentType,
            String documentNumber,
            java.time.LocalDate birthDate,
            MaritalStatus maritalStatus,
            Integer childrenCount,
            Integer housesCount,
            Boolean hasPartner,
            String companyName,
            String officeName,
            Integer employeesCount,
            StylePreference stylePreference,
            com.scalaris.auth.domain.TaxPosition taxPosition,
            com.scalaris.auth.domain.CompanyStructure companyStructure,
            String notes
    ) {
        tp.setKind(kind);
        tp.setPersonType(personType);
        tp.setDisplayName(displayName == null ? null : displayName.trim());
        tp.setLegalName(trimOrNull(legalName));
        tp.setEmail(trimOrNull(email));
        tp.setPhone(trimOrNull(phone));
        tp.setDocumentType(trimOrNull(documentType));
        tp.setDocumentNumber(trimOrNull(documentNumber));
        tp.setBirthDate(birthDate);
        tp.setMaritalStatus(maritalStatus);
        tp.setChildrenCount(childrenCount);
        tp.setHousesCount(housesCount);
        tp.setHasPartner(hasPartner);
        tp.setCompanyName(trimOrNull(companyName));
        tp.setOfficeName(trimOrNull(officeName));
        tp.setEmployeesCount(employeesCount);
        tp.setStylePreference(stylePreference);
        tp.setTaxPosition(taxPosition);
        tp.setCompanyStructure(companyStructure);
        tp.setNotes(trimOrNull(notes));
    }

    private void enforceUniqueness(ThirdParty tp, UUID excludeId) {
        if (tpEmail(tp) != null) {
            if (repo.existsActiveEmail(tpEmail(tp), excludeId)) {
                throw new ConflictException("Email ya registrado en clientes/proveedores");
            }
        }
        if (tpDocType(tp) != null && tpDocNumber(tp) != null) {
            if (repo.existsActiveDocument(tpDocType(tp), tpDocNumber(tp), excludeId)) {
                throw new ConflictException("Documento ya registrado en clientes/proveedores");
            }
        }
    }

    private void syncAddresses(ThirdParty tp, List<ThirdPartyCreateRequest.AddressDto> dtos) {
        tp.getAddresses().clear();
        if (dtos == null) return;

        for (var dto : dtos) {
            var a = new ThirdPartyAddress(tp, dto.type(), dto.line1().trim());
            a.setLine2(trimOrNull(dto.line2()));
            a.setCity(trimOrNull(dto.city()));
            a.setState(trimOrNull(dto.state()));
            a.setZip(trimOrNull(dto.zip()));
            a.setCountry(trimOrNull(dto.country()));
            a.setPrimary(dto.primary());
            tp.getAddresses().add(a);
        }
    }

    /**
     * TaxId tiene unique global (type,value). Para evitar quilombo de inserts/deletes,
     * reusamos entidades existentes cuando coinciden (type,value).
     */
    private void syncTaxIds(ThirdParty tp, List<ThirdPartyCreateRequest.TaxIdDto> dtos) {
        var existing = new HashMap<String, ThirdPartyTaxId>();
        for (var e : tp.getTaxIds()) {
            existing.put(key(e.getTaxIdType(), e.getValue()), e);
        }

        var newList = new ArrayList<ThirdPartyTaxId>();
        if (dtos != null) {
            for (var dto : dtos) {
                String norm = normalize(dto.value());
                String k = key(dto.type(), norm);
                var entity = existing.get(k);
                if (entity == null) entity = new ThirdPartyTaxId(tp, dto.type(), norm);
                entity.setPrimary(dto.primary());
                newList.add(entity);
            }
        }

        tp.getTaxIds().clear();
        tp.getTaxIds().addAll(newList);
    }

    private void enforcePrimaryRules(ThirdParty tp) {
        // addresses: max 1 primary (si querés por tipo, cambialo)
        long addrPrimary = tp.getAddresses().stream().filter(ThirdPartyAddress::isPrimary).count();
        if (addrPrimary > 1) throw new IllegalArgumentException("Solo una dirección puede ser primaria");

        // taxIds: max 1 primary
        long taxPrimary = tp.getTaxIds().stream().filter(ThirdPartyTaxId::isPrimary).count();
        if (taxPrimary > 1) throw new IllegalArgumentException("Solo un TaxId puede ser primario");
    }

    private static String key(TaxIdType type, String value) {
        return type.name() + "|" + normalize(value);
    }
    private static String normalize(String s) {
        return s == null ? null : s.trim().replace(" ", "");
    }
    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isBlank() ? null : t;
    }

    // helpers por si no agregaste getters en ThirdParty aún (recomendado agregarlos)
    private static String tpEmail(ThirdParty tp) {
        try { return (String) ThirdParty.class.getMethod("getEmail").invoke(tp); }
        catch (Exception e) { return null; }
    }
    private static String tpDocType(ThirdParty tp) {
        try { return (String) ThirdParty.class.getMethod("getDocumentType").invoke(tp); }
        catch (Exception e) { return null; }
    }
    private static String tpDocNumber(ThirdParty tp) {
        try { return (String) ThirdParty.class.getMethod("getDocumentNumber").invoke(tp); }
        catch (Exception e) { return null; }
    }
}
