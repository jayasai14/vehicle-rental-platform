package com.vehiclerental.modules.booking.entity;

import com.vehiclerental.modules.location.entity.PickupLocation;
import com.vehiclerental.modules.user.entity.User;
import com.vehiclerental.modules.vehicle.entity.Vehicle;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_holds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingHold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hold_reference", nullable = false, unique = true, length = 32)
    private String holdReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_user_id", nullable = false)
    private User customerUser;

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

    @Column(name = "quoted_rental_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal quotedRentalAmount;

    @Column(name = "quoted_deposit_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal quotedDepositAmount;

    @Column(name = "quoted_surcharge_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal quotedSurchargeAmount;

    @Column(name = "total_hold_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalHoldAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HoldStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recovered_from_hold_id")
    private BookingHold recoveredFromHold;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (quotedSurchargeAmount == null) quotedSurchargeAmount = BigDecimal.ZERO;
        if (status == null) status = HoldStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum HoldStatus {
        ACTIVE,
        VERIFICATION_PENDING,
        CONVERTED,
        EXPIRED,
        RELEASED
    }
}
