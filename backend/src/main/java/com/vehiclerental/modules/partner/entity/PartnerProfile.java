package com.vehiclerental.modules.partner.entity;

import com.vehiclerental.modules.user.entity.Address;
import com.vehiclerental.modules.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "partner_profiles")
public class PartnerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "business_name", nullable = false, length = 200)
    private String businessName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_business_address_id")
    private Address registeredBusinessAddress;

    @Column(name = "pan_number", nullable = false, unique = true, length = 10)
    private String panNumber;

    @Column(length = 15)
    private String gstin;

    @Column(name = "trade_licence_number", length = 50)
    private String tradeLicenceNumber;

    @Column(name = "commercial_registration_number", length = 50)
    private String commercialRegistrationNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    private PartnerVerificationStatus verificationStatus = PartnerVerificationStatus.SUBMITTED;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_user_id")
    private User verifiedByUser;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum PartnerVerificationStatus {
        SUBMITTED, IN_REVIEW, VERIFIED, REJECTED
    }

    public PartnerProfile() {}

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public Address getRegisteredBusinessAddress() { return registeredBusinessAddress; }
    public void setRegisteredBusinessAddress(Address registeredBusinessAddress) { this.registeredBusinessAddress = registeredBusinessAddress; }
    public String getPanNumber() { return panNumber; }
    public void setPanNumber(String panNumber) { this.panNumber = panNumber; }
    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }
    public String getTradeLicenceNumber() { return tradeLicenceNumber; }
    public void setTradeLicenceNumber(String tradeLicenceNumber) { this.tradeLicenceNumber = tradeLicenceNumber; }
    public String getCommercialRegistrationNumber() { return commercialRegistrationNumber; }
    public void setCommercialRegistrationNumber(String commercialRegistrationNumber) { this.commercialRegistrationNumber = commercialRegistrationNumber; }
    public PartnerVerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(PartnerVerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    public User getVerifiedByUser() { return verifiedByUser; }
    public void setVerifiedByUser(User verifiedByUser) { this.verifiedByUser = verifiedByUser; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
