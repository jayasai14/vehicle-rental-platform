package com.vehiclerental.modules.user.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_verifications")
public class KycVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_type", nullable = false)
    private KycType kycType = KycType.AADHAAR_OKYC;

    @Column(name = "verification_reference", nullable = false, length = 100)
    private String verificationReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus status = KycStatus.INITIATED;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "id_card_storage_key")
    private String idCardStorageKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum KycType {
        AADHAAR_OKYC, PASSPORT, VOTER_ID
    }

    public enum KycStatus {
        INITIATED, VERIFIED, FAILED
    }

    public KycVerification() {}

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public KycType getKycType() { return kycType; }
    public void setKycType(KycType kycType) { this.kycType = kycType; }
    public String getVerificationReference() { return verificationReference; }
    public void setVerificationReference(String verificationReference) { this.verificationReference = verificationReference; }
    public KycStatus getStatus() { return status; }
    public void setStatus(KycStatus status) { this.status = status; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    public String getIdCardStorageKey() { return idCardStorageKey; }
    public void setIdCardStorageKey(String idCardStorageKey) { this.idCardStorageKey = idCardStorageKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
