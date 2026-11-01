package com.vehiclerental.modules.payment.entity;

import com.vehiclerental.modules.partner.entity.PartnerProfile;
import com.vehiclerental.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rental_payment_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalPaymentSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "submission_reference", nullable = false, unique = true, length = 32)
    private String submissionReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demand_id", nullable = false)
    private RentalPaymentDemand demand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_partner_id", nullable = false)
    private PartnerProfile managerPartner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_user_id", nullable = false)
    private User customerUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "transaction_reference", length = 100)
    private String transactionReference;

    @Column(name = "amount_submitted", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountSubmitted;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "receipt_object_key", length = 255)
    private String receiptObjectKey;

    @Column(name = "transaction_timestamp", nullable = false)
    private LocalDateTime transactionTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_status", nullable = false, length = 30)
    private SubmissionStatus submissionStatus;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_user_id")
    private User verifiedByUser;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recovered_for_demand_id")
    private RentalPaymentDemand recoveredForDemand;

    @Column(name = "reapplied_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal reappliedAmount;

    @Column(name = "reapplied_at")
    private LocalDateTime reappliedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (currency == null) currency = "INR";
        if (submissionStatus == null) submissionStatus = SubmissionStatus.SUBMITTED_FOR_VERIFICATION;
        if (reappliedAmount == null) reappliedAmount = BigDecimal.ZERO;
        if (transactionTimestamp == null) transactionTimestamp = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum PaymentMethod {
        UPI_QR,
        UPI_ID,
        EXTERNAL_LINK,
        CASH_AT_PICKUP
    }

    public enum SubmissionStatus {
        SUBMITTED_FOR_VERIFICATION,
        VERIFIED,
        REJECTED
    }
}
