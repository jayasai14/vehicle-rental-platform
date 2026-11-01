package com.vehiclerental.modules.payment.entity;

import com.vehiclerental.modules.booking.entity.Booking;
import com.vehiclerental.modules.partner.entity.PartnerProfile;
import com.vehiclerental.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "security_deposits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityDeposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_partner_id", nullable = false)
    private PartnerProfile managerPartner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_user_id", nullable = false)
    private User customerUser;

    @Column(name = "required_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal requiredAmount;

    @Column(name = "collected_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal collectedAmount;

    @Column(name = "held_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal heldAmount;

    @Column(name = "deducted_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal deductedAmount;

    @Column(name = "refunded_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal refundedAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "collection_method", nullable = false, length = 30)
    private CollectionMethod collectionMethod;

    @Column(name = "collection_transaction_reference", length = 100)
    private String collectionTransactionReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 35)
    private DepositStatus status;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (currency == null) currency = "INR";
        if (collectedAmount == null) collectedAmount = BigDecimal.ZERO;
        if (heldAmount == null) heldAmount = BigDecimal.ZERO;
        if (deductedAmount == null) deductedAmount = BigDecimal.ZERO;
        if (refundedAmount == null) refundedAmount = BigDecimal.ZERO;
        if (status == null) status = DepositStatus.PENDING_COLLECTION;
        if (collectionMethod == null) collectionMethod = CollectionMethod.UPI_TRANSFER;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum CollectionMethod {
        UPI_TRANSFER,
        BANK_NEFT_IMPS,
        CASH_AT_PICKUP
    }

    public enum DepositStatus {
        PENDING_COLLECTION,
        HELD_BY_PARTNER,
        PARTIALLY_REFUNDED,
        FULLY_REFUNDED,
        FORFEITED_TO_COVER_LOSSES
    }
}
