package com.vehiclerental.modules.vehicle.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_pricing_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehiclePricingProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false, unique = true)
    private Vehicle vehicle;

    @Column(name = "hourly_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @Column(name = "daily_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyRate;

    @Column(name = "weekly_rate", precision = 10, scale = 2)
    private BigDecimal weeklyRate;

    @Column(name = "security_deposit_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal securityDepositAmount;

    @Column(name = "free_km_per_day", nullable = false)
    private Integer freeKmPerDay;

    @Column(name = "extra_km_charge", nullable = false, precision = 10, scale = 2)
    private BigDecimal extraKmCharge;

    @Column(name = "late_return_hourly_penalty", nullable = false, precision = 10, scale = 2)
    private BigDecimal lateReturnHourlyPenalty;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (securityDepositAmount == null) securityDepositAmount = new BigDecimal("2000.00");
        if (freeKmPerDay == null) freeKmPerDay = 300;
        if (extraKmCharge == null) extraKmCharge = new BigDecimal("10.00");
        if (lateReturnHourlyPenalty == null) lateReturnHourlyPenalty = new BigDecimal("200.00");
        if (isActive == null) isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
