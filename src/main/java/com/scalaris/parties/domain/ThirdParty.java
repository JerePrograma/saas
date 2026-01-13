package com.scalaris.parties.domain;

import com.scalaris.auth.domain.CompanyStructure;
import com.scalaris.auth.domain.TaxPosition;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "third_party",
        indexes = {
                @Index(name = "ix_tp_kind", columnList = "kind"),
                @Index(name = "ix_tp_active", columnList = "active")
        })
public class ThirdParty {

    @Id @GeneratedValue @UuidGenerator
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ThirdPartyKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "person_type", nullable = false, length = 10)
    private PersonType personType;

    // “Nombre y apellido / Razón social” (lo que se muestra siempre)
    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    // opcional: razón social/legal exacta
    @Column(name = "legal_name", length = 160)
    private String legalName;

    @Column(length = 254)
    private String email;

    @Column(length = 40)
    private String phone;

    // Documento “único” (si aplica)
    @Column(name = "document_type", length = 20)
    private String documentType;

    @Column(name = "document_number", length = 40)
    private String documentNumber;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "marital_status", length = 15)
    private MaritalStatus maritalStatus;

    @Column(name = "children_count")
    private Integer childrenCount;

    @Column(name = "houses_count")
    private Integer housesCount;

    @Column(name = "has_partner")
    private Boolean hasPartner;

    // QA: “empresa, oficina, empleados”
    @Column(name = "company_name", length = 160)
    private String companyName;

    @Column(name = "office_name", length = 160)
    private String officeName;

    @Column(name = "employees_count")
    private Integer employeesCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "style_preference", length = 15)
    private StylePreference stylePreference;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_position", length = 30)
    private TaxPosition taxPosition;

    @Enumerated(EnumType.STRING)
    @Column(name = "company_structure", length = 30)
    private CompanyStructure companyStructure;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "thirdParty", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ThirdPartyAddress> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "thirdParty", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ThirdPartyTaxId> taxIds = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ThirdParty() {}

    @PrePersist void onCreate() {
        var now = OffsetDateTime.now();
        createdAt = now; updatedAt = now;
        normalizeEmail();
    }
    @PreUpdate void onUpdate() {
        updatedAt = OffsetDateTime.now();
        normalizeEmail();
    }
    private void normalizeEmail() {
        if (email != null) email = email.trim().toLowerCase();
    }

    public UUID getId() { return id; }
    public boolean isActive() { return active; }
    public void deactivate() { this.active = false; }

    // setters mínimos para service (sin “magia”)
    public void setKind(ThirdPartyKind kind) { this.kind = kind; }
    public void setPersonType(PersonType personType) { this.personType = personType; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public void setMaritalStatus(MaritalStatus maritalStatus) { this.maritalStatus = maritalStatus; }
    public void setChildrenCount(Integer childrenCount) { this.childrenCount = childrenCount; }
    public void setHousesCount(Integer housesCount) { this.housesCount = housesCount; }
    public void setHasPartner(Boolean hasPartner) { this.hasPartner = hasPartner; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public void setOfficeName(String officeName) { this.officeName = officeName; }
    public void setEmployeesCount(Integer employeesCount) { this.employeesCount = employeesCount; }
    public void setStylePreference(StylePreference stylePreference) { this.stylePreference = stylePreference; }
    public void setTaxPosition(TaxPosition taxPosition) { this.taxPosition = taxPosition; }
    public void setCompanyStructure(CompanyStructure companyStructure) { this.companyStructure = companyStructure; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<ThirdPartyAddress> getAddresses() { return addresses; }
    public List<ThirdPartyTaxId> getTaxIds() { return taxIds; }
}
