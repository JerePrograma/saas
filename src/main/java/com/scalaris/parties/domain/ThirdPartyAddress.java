package com.scalaris.parties.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "third_party_address",
        indexes = @Index(name = "ix_tpa_party", columnList = "third_party_id"))
public class ThirdPartyAddress {

    @Id @GeneratedValue @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "third_party_id", nullable = false)
    private ThirdParty thirdParty;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", nullable = false, length = 15)
    private AddressType addressType;

    @Column(name = "line1", nullable = false, length = 200)
    private String line1;

    @Column(name = "line2", length = 200)
    private String line2;

    @Column(length = 80) private String city;
    @Column(length = 80) private String state;
    @Column(length = 20) private String zip;
    @Column(length = 80) private String country;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ThirdPartyAddress() {}

    public ThirdPartyAddress(ThirdParty thirdParty, AddressType type, String line1) {
        this.thirdParty = thirdParty;
        this.addressType = type;
        this.line1 = line1;
    }

    @PrePersist void onCreate() { createdAt = OffsetDateTime.now(); }
}
