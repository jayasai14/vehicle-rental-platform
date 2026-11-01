package com.vehiclerental.modules.payment.entity;

import com.vehiclerental.modules.booking.entity.Booking;
import com.vehiclerental.modules.booking.entity.BookingHold;
import com.vehiclerental.modules.operations.entity.SupportTicket;
import com.vehiclerental.modules.partner.entity.PartnerProfile;
import com.vehiclerental.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rental_fee_refunds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalFeeRefund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "refund_reference", nullable = false, unique = true, length = 32)
    private String refundReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demand_id")
    private RentalPaymentDemand demand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hold_id")
    private BookingHold hold;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_partner_id", nullable = false)
    private PartnerProfile managerPartner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_user_id", nullable = false)
    private User customerUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_reason", nullable = false, length = 40)
    private RefundReason refundReason;

    @Column(name = "cancellation_policy_snapshot", columnDefinition = "JSON")
    private String cancellationPolicySnapshot;

    @Column(name = "gross_amount_paid", nullable = false, precision = 10, scale = 2)
    private BigDecimal grossAmountPaid;

    @Column(name = "cancellation_fee_deducted", nullable = false, precision = 10, scale = 2)
    private BigDecimal cancellationFeeDeducted;

    @Column(name = "refund_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", nullable = false, length = 45)
    private RefundStatus refundStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_method", nullable = false, length = 30)
    private RefundMethod refundMethod;

    @Column(name = "partner_transaction_reference", length = 100)
    private String partnerTransactionReference;

    @Column(name = "receipt_object_key", length = 255)
    private String receiptObjectKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_user_id", nullable = false)
    private User recordedByUser;

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

    @Column(name = "disbursed_at")
    private LocalDateTime disbursedAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "dispute_reason", length = 500)
    private String disputeReason;

    @Column(name = "dispute_opened_at")
    private LocalDateTime disputeOpenedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (currency == null) currency = "INR";
        if (grossAmountPaid == null) grossAmountPaid = BigDecimal.ZERO;
        if (cancellationFeeDeducted == null) cancellationFeeDeducted = BigDecimal.ZERO;
        if (refundStatus == null) refundStatus = RefundStatus.PENDING_PARTNER_DISBURSEMENT;
        if (refundMethod == null) refundMethod = RefundMethod.UPI_TRANSFER;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum RefundReason {
        BOOKING_CANCELLATION,
        OVERPAYMENT_RETURN,
        EXPIRED_HOLD_EXCEPTION,
        PAYMENT_MISMATCH_RETURN,
        ADMIN_ARBITRATION_ORDER,
        OTHER_EXCEPTION
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

    public enum RefundMethod {
        UPI_TRANSFER,
        BANK_NEFT_IMPS,
        CASH_RETURN
    }
}
