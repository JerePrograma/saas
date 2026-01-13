package com.scalaris.auth.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "app_user",
        uniqueConstraints = @UniqueConstraint(name = "uk_app_user_email", columnNames = "email")
)
public class User {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 40)
    private String fullName;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 120)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.EMPLOYEE;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_position", length = 30)
    private TaxPosition taxPosition;

    @Enumerated(EnumType.STRING)
    @Column(name = "company_structure", length = 30)
    private CompanyStructure companyStructure;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "accepted_terms", nullable = false)
    private boolean acceptedTerms;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected User() {}

    public User(String fullName,
                String email,
                String passwordHash,
                UserRole role,
                TaxPosition taxPosition,
                CompanyStructure companyStructure,
                boolean acceptedTerms) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role == null ? UserRole.EMPLOYEE : role;
        this.taxPosition = taxPosition;
        this.companyStructure = companyStructure;
        this.acceptedTerms = acceptedTerms;
    }

    @PrePersist
    void onCreate() {
        normalizeEmail();
        var now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        normalizeEmail();
        this.updatedAt = OffsetDateTime.now();
    }

    private void normalizeEmail() {
        if (this.email != null) this.email = this.email.trim().toLowerCase();
    }

    // Getters (sin Lombok, sin magia)
    public UUID getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public UserRole getRole() { return role; }
    public TaxPosition getTaxPosition() { return taxPosition; }
    public CompanyStructure getCompanyStructure() { return companyStructure; }
    public boolean isActive() { return active; }
    public boolean isAcceptedTerms() { return acceptedTerms; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setTaxPosition(TaxPosition taxPosition) { this.taxPosition = taxPosition; }
    public void setCompanyStructure(CompanyStructure companyStructure) { this.companyStructure = companyStructure; }
}
