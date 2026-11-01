package com.vehiclerental.modules.partner.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "partner_payment_profiles")
public class PartnerPaymentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false, unique = true)
    private PartnerProfile partner;

    @Column(name = "payee_display_name", nullable = false, length = 150)
    private String payeeDisplayName;

    @Column(name = "upi_id", nullable = false, length = 100)
    private String upiId;

    @Column(name = "upi_qr_object_key")
    private String upiQrObjectKey;

    @Column(name = "external_payment_url")
    private String externalPaymentUrl;

    @Column(name = "accepted_payment_methods", nullable = false, columnDefinition = "JSON")
    private String acceptedPaymentMethods = "[\"UPI_QR\", \"UPI_ID\", \"CASH_AT_PICKUP\"]";

    @Column(name = "is_cash_at_pickup_enabled", nullable = false)
    private boolean cashAtPickupEnabled = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public PartnerPaymentProfile() {}

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PartnerProfile getPartner() { return partner; }
    public void setPartner(PartnerProfile partner) { this.partner = partner; }
    public String getPayeeDisplayName() { return payeeDisplayName; }
    public void setPayeeDisplayName(String payeeDisplayName) { this.payeeDisplayName = payeeDisplayName; }
    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }
    public String getUpiQrObjectKey() { return upiQrObjectKey; }
    public void setUpiQrObjectKey(String upiQrObjectKey) { this.upiQrObjectKey = upiQrObjectKey; }
    public String getExternalPaymentUrl() { return externalPaymentUrl; }
    public void setExternalPaymentUrl(String externalPaymentUrl) { this.externalPaymentUrl = externalPaymentUrl; }
    public String getAcceptedPaymentMethods() { return acceptedPaymentMethods; }
    public void setAcceptedPaymentMethods(String acceptedPaymentMethods) { this.acceptedPaymentMethods = acceptedPaymentMethods; }
    public boolean isCashAtPickupEnabled() { return cashAtPickupEnabled; }
    public void setCashAtPickupEnabled(boolean cashAtPickupEnabled) { this.cashAtPickupEnabled = cashAtPickupEnabled; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
