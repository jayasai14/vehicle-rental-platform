package com.vehiclerental.modules.vehicle.entity;

import com.vehiclerental.modules.location.entity.PartnerHub;
import com.vehiclerental.modules.partner.entity.PartnerProfile;
import com.vehiclerental.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registration_number", nullable = false, unique = true, length = 30)
    private String registrationNumber;

    @Column(name = "vin_chassis_number", nullable = false, unique = true, length = 50)
    private String vinChassisNumber;

    @Column(name = "engine_number", nullable = false, unique = true, length = 50)
    private String engineNumber;

    @Column(nullable = false, length = 50)
    private String make;

    @Column(nullable = false, length = 50)
    private String model;

    @Column(name = "year_of_manufacture", nullable = false)
    private Integer yearOfManufacture;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VehicleCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private TransmissionType transmission = TransmissionType.MANUAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false, length = 30)
    @Builder.Default
    private FuelType fuelType = FuelType.PETROL;

    @Column(name = "seating_capacity", nullable = false)
    @Builder.Default
    private Integer seatingCapacity = 4;

    @Column(name = "odometer_reading_km", nullable = false)
    @Builder.Default
    private Integer odometerReadingKm = 0;

    @Column(name = "current_fuel_or_battery_level_percent", nullable = false)
    @Builder.Default
    private Integer currentFuelOrBatteryLevelPercent = 100;

    @Enumerated(EnumType.STRING)
    @Column(name = "ownership_type", nullable = false, length = 30)
    private OwnershipType ownershipType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_owner_user_id")
    private User legalOwnerUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_owner_partner_id")
    private PartnerProfile legalOwnerPartner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_partner_id", nullable = false)
    private PartnerProfile managerPartner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_hub_id")
    private PartnerHub assignedHub;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_custody_status", nullable = false, length = 30)
    @Builder.Default
    private CustodyStatus currentCustodyStatus = CustodyStatus.PARTNER_CUSTODY;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_status", nullable = false, length = 30)
    @Builder.Default
    private ListingStatus listingStatus = ListingStatus.DRAFT;

    @Column(name = "turnaround_buffer_minutes", nullable = false)
    @Builder.Default
    private Integer turnaroundBufferMinutes = 60;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum VehicleCategory {
        CAR, BIKE, SCOOTER
    }

    public enum TransmissionType {
        MANUAL, AUTOMATIC
    }

    public enum FuelType {
        PETROL, DIESEL, ELECTRIC, HYBRID, CNG
    }

    public enum OwnershipType {
        INDIVIDUAL_OWNER, PARTNER_BUSINESS_OWNER
    }

    public enum CustodyStatus {
        CONTRIBUTOR_CUSTODY, PARTNER_CUSTODY, CUSTOMER_CUSTODY, IN_MAINTENANCE, IN_TRANSIT
    }

    public enum ListingStatus {
        DRAFT, PENDING_APPROVAL, PUBLISHED, UNLISTED, SUSPENDED
    }
}
