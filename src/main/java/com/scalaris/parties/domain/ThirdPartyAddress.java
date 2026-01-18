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

    // getters
    public UUID getId() { return id; }
    public AddressType getAddressType() { return addressType; }
    public String getLine1() { return line1; }
    public String getLine2() { return line2; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getZip() { return zip; }
    public String getCountry() { return country; }
    public boolean isPrimary() { return primary; }

    // setters (para service)
    public void setLine2(String line2) { this.line2 = line2; }
    public void setCity(String city) { this.city = city; }
    public void setState(String state) { this.state = state; }
    public void setZip(String zip) { this.zip = zip; }
    public void setCountry(String country) { this.country = country; }
    public void setPrimary(boolean primary) { this.primary = primary; }
}
