package com.vehiclerental.modules.booking.entity;

import com.vehiclerental.modules.location.entity.PartnerHub;
import com.vehiclerental.modules.user.entity.User;
import com.vehiclerental.modules.vehicle.entity.Vehicle;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inspections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(name = "inspection_type", nullable = false, length = 40)
    private InspectionType inspectionType;

    @Column(name = "odometer_reading_km", nullable = false)
    private Integer odometerReadingKm;

    @Column(name = "fuel_or_battery_level_percent", nullable = false)
    private Integer fuelOrBatteryLevelPercent;

    @Lob
    @Column(name = "exterior_condition_notes")
    private String exteriorConditionNotes;

    @Lob
    @Column(name = "interior_condition_notes")
    private String interiorConditionNotes;

    @Lob
    @Column(name = "known_scratches_or_dents")
    private String knownScratchesOrDents;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspector_user_id", nullable = false)
    private User inspectorUser;

    @Column(name = "customer_signature_confirmed", nullable = false)
    private Boolean customerSignatureConfirmed;

    @Column(name = "partner_signature_confirmed", nullable = false)
    private Boolean partnerSignatureConfirmed;

    @Column(name = "conducted_at", nullable = false)
    private LocalDateTime conductedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hub_id")
    private PartnerHub hub;

    @PrePersist
    protected void onCreate() {
        if (conductedAt == null) conductedAt = LocalDateTime.now();
        if (customerSignatureConfirmed == null) customerSignatureConfirmed = false;
        if (partnerSignatureConfirmed == null) partnerSignatureConfirmed = false;
    }

    public enum InspectionType {
        PRE_RENTAL_HANDOVER,
        POST_RENTAL_RETURN,
        MAINTENANCE_INSPECTION
    }
}
