package com.scalaris.parties.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "third_party_tax_id",
        indexes = @Index(name = "ix_tpt_party", columnList = "third_party_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_tpt_type_value", columnNames = {"tax_id_type", "value"}))
public class ThirdPartyTaxId {

    @Id @GeneratedValue @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "third_party_id", nullable = false)
    private ThirdParty thirdParty;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_id_type", nullable = false, length = 15)
    private TaxIdType taxIdType;

    @Column(nullable = false, length = 40)
    private String value;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ThirdPartyTaxId() {}

    public ThirdPartyTaxId(ThirdParty thirdParty, TaxIdType type, String value) {
        this.thirdParty = thirdParty;
        this.taxIdType = type;
        this.value = value;
    }

    @PrePersist void onCreate() { createdAt = OffsetDateTime.now(); }

    public UUID getId() { return id; }
    public TaxIdType getTaxIdType() { return taxIdType; }
    public String getValue() { return value; }
    public boolean isPrimary() { return primary; }
    public void setPrimary(boolean primary) { this.primary = primary; }
}
