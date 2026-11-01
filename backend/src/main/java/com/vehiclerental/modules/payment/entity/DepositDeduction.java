package com.vehiclerental.modules.payment.entity;

import com.vehiclerental.modules.booking.entity.Incident;
import com.vehiclerental.modules.booking.entity.Inspection;
import com.vehiclerental.modules.operations.entity.SupportTicket;
import com.vehiclerental.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "deposit_deductions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepositDeduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposit_id", nullable = false)
    private SecurityDeposit deposit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_id")
    private Inspection inspection;

    @Enumerated(EnumType.STRING)
    @Column(name = "deduction_reason", nullable = false, length = 40)
    private DeductionReason deductionReason;

    @Column(name = "proposed_deduction_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal proposedDeductionAmount;

    @Column(name = "approved_deduction_amount", precision = 10, scale = 2)
    private BigDecimal approvedDeductionAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposed_by_user_id", nullable = false)
    private User proposedByUser;

    @Column(name = "evidence_document_key", length = 255)
    private String evidenceDocumentKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeductionStatus status;

    @Column(name = "customer_dispute_reason", length = 500)
    private String customerDisputeReason;

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
        if (status == null) status = DeductionStatus.PROPOSED;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum DeductionReason {
        VEHICLE_DAMAGE,
        FUEL_DEFICIT,
        LATE_RETURN_PENALTY,
        TRAFFIC_VIOLATION_FINE,
        CLEANING_FEE
    }

    public enum DeductionStatus {
        PROPOSED,
        DISPUTED,
        APPROVED_BY_ADMIN,
        AGREED_BY_CUSTOMER,
        CANCELLED
    }
}
