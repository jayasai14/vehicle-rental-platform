package com.vehiclerental.modules.payment.entity;

import com.vehiclerental.modules.booking.entity.BookingHold;
import com.vehiclerental.modules.partner.entity.PartnerProfile;
import com.vehiclerental.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rental_payment_demands")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalPaymentDemand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "demand_reference", nullable = false, unique = true, length = 32)
    private String demandReference;

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
    @Column(name = "demand_type", nullable = false, length = 30)
    private DemandType demandType;

    @Column(name = "amount_demanded", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountDemanded;

    @Column(name = "total_verified_submitted", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalVerifiedSubmitted;

    @Column(name = "amount_satisfied", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountSatisfied;

    @Column(name = "excess_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal excessAmount;

    @Column(name = "cancellation_fee_retained", nullable = false, precision = 10, scale = 2)
    private BigDecimal cancellationFeeRetained;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DemandStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (totalVerifiedSubmitted == null) totalVerifiedSubmitted = BigDecimal.ZERO;
        if (amountSatisfied == null) amountSatisfied = BigDecimal.ZERO;
        if (excessAmount == null) excessAmount = BigDecimal.ZERO;
        if (cancellationFeeRetained == null) cancellationFeeRetained = BigDecimal.ZERO;
        if (currency == null) currency = "INR";
        if (status == null) status = DemandStatus.OPEN;
        if (demandType == null) demandType = DemandType.BOOKING_CONFIRMATION;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum DemandType {
        BOOKING_CONFIRMATION,
        BOOKING_EXTENSION,
        LATE_RETURN_FEE,
        INCIDENT_CHARGE
    }

    public enum DemandStatus {
        OPEN,
        PARTIALLY_SATISFIED,
        SATISFIED,
        EXPIRED,
        CANCELLED
    }
}
