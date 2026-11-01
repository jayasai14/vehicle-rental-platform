package com.vehiclerental.modules.payment.entity;

import com.vehiclerental.modules.operations.entity.SupportTicket;
import com.vehiclerental.modules.partner.entity.PartnerProfile;
import com.vehiclerental.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "security_deposit_refunds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityDepositRefund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposit_id", nullable = false)
    private SecurityDeposit deposit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_partner_id", nullable = false)
    private PartnerProfile managerPartner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_user_id", nullable = false)
    private User customerUser;

    @Column(name = "refund_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_method", nullable = false, length = 30)
    private RefundMethod refundMethod;

    @Column(name = "partner_transaction_reference", length = 100)
    private String partnerTransactionReference;

    @Column(name = "receipt_object_key", length = 255)
    private String receiptObjectKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", nullable = false, length = 45)
    private RefundStatus refundStatus;

    @Column(name = "disbursed_at")
    private LocalDateTime disbursedAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "dispute_reason", length = 500)
    private String disputeReason;

    @Column(name = "dispute_opened_at")
    private LocalDateTime disputeOpenedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispute_ticket_id")
    private SupportTicket disputeTicket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_admin_user_id")
    private User resolvedByAdminUser;

    @Column(name = "resolution_notes", length = 1000)
    private String resolutionNotes;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (refundStatus == null) refundStatus = RefundStatus.PENDING_PARTNER_DISBURSEMENT;
        if (refundMethod == null) refundMethod = RefundMethod.UPI_TRANSFER;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum RefundMethod {
        UPI_TRANSFER,
        BANK_NEFT_IMPS,
        CASH_RETURN
    }

    public enum RefundStatus {
        PENDING_PARTNER_DISBURSEMENT,
        DISBURSED_BY_PARTNER,
        CUSTOMER_ACKNOWLEDGED,
        DISPUTED_BY_CUSTOMER,
        DISPUTE_WINDOW_ELAPSED_UNACKNOWLEDGED,
        ADMIN_RESOLVED,
        CANCELLED
    }
}
