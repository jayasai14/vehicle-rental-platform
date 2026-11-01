package com.vehiclerental.modules.payment.entity;

import com.vehiclerental.modules.booking.entity.Booking;
import com.vehiclerental.modules.contributor.entity.ContributionAgreement;
import com.vehiclerental.modules.partner.entity.PartnerProfile;
import com.vehiclerental.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "contributor_payables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContributorPayable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_id", nullable = false)
    private ContributionAgreement agreement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private PartnerProfile partner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contributor_user_id", nullable = false)
    private User contributorUser;

    @Column(name = "agreed_cut_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal agreedCutAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payable_status", nullable = false, length = 35)
    private PayableStatus payableStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "partner_payment_method", length = 30)
    private PartnerPaymentMethod partnerPaymentMethod;

    @Column(name = "partner_transaction_reference", length = 100)
    private String partnerTransactionReference;

    @Column(name = "partner_receipt_object_key", length = 255)
    private String partnerReceiptObjectKey;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "dispute_reason", length = 500)
    private String disputeReason;

    @Column(name = "disputed_at")
    private LocalDateTime disputedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (payableStatus == null) payableStatus = PayableStatus.PENDING_COMPLETION;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum PayableStatus {
        PENDING_COMPLETION,
        ELIGIBLE,
        SETTLEMENT_RECORDED,
        CONFIRMED_BY_CONTRIBUTOR,
        DISPUTED
    }

    public enum PartnerPaymentMethod {
        BANK_NEFT_IMPS,
        UPI_TRANSFER
    }
}
