package com.vehiclerental.modules.booking.entity;

import com.vehiclerental.modules.location.entity.PickupLocation;
import com.vehiclerental.modules.partner.entity.PartnerProfile;
import com.vehiclerental.modules.payment.entity.RentalPaymentDemand;
import com.vehiclerental.modules.user.entity.User;
import com.vehiclerental.modules.vehicle.entity.Vehicle;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_code", nullable = false, unique = true, length = 32)
    private String bookingCode;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hold_id", nullable = false, unique = true)
    private BookingHold hold;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_user_id", nullable = false)
    private User customerUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_partner_id", nullable = false)
    private PartnerProfile managerPartner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_location_id", nullable = false)
    private PickupLocation pickupLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dropoff_location_id", nullable = false)
    private PickupLocation dropoffLocation;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "actual_return_time")
    private LocalDateTime actualReturnTime;

    @Column(name = "base_rental_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseRentalAmount;

    @Column(name = "security_deposit_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal securityDepositAmount;

    @Column(name = "delivery_surcharge_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal deliverySurchargeAmount;

    @Column(name = "total_amount_payable", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmountPayable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initial_demand_id", nullable = false)
    private RentalPaymentDemand initialDemand;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status", nullable = false, length = 30)
    private BookingStatus bookingStatus;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (deliverySurchargeAmount == null) deliverySurchargeAmount = BigDecimal.ZERO;
        if (bookingStatus == null) bookingStatus = BookingStatus.CONFIRMED;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum BookingStatus {
        CONFIRMED,
        READY_FOR_PICKUP,
        IN_PROGRESS,
        OVERDUE,
        RETURNED,
        COMPLETED,
        CANCELLED,
        NO_SHOW
    }
}
