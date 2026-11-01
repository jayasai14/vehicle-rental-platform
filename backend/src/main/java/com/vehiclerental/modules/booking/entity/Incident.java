package com.vehiclerental.modules.booking.entity;

import com.vehiclerental.modules.user.entity.User;
import com.vehiclerental.modules.vehicle.entity.Vehicle;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_reference", nullable = false, unique = true, length = 32)
    private String incidentReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_user_id", nullable = false)
    private User reportedByUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "incident_type", nullable = false, length = 40)
    private IncidentType incidentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentSeverity severity;

    @Lob
    @Column(nullable = false)
    private String description;

    @Column(name = "police_report_filed", nullable = false)
    private Boolean policeReportFiled;

    @Column(name = "police_fir_number", length = 100)
    private String policeFirNumber;

    @Column(name = "estimated_damage_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal estimatedDamageCost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IncidentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (policeReportFiled == null) policeReportFiled = false;
        if (estimatedDamageCost == null) estimatedDamageCost = BigDecimal.ZERO;
        if (severity == null) severity = IncidentSeverity.MEDIUM;
        if (status == null) status = IncidentStatus.REPORTED;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum IncidentType {
        ACCIDENT_MAJOR,
        ACCIDENT_MINOR,
        MECHANICAL_BREAKDOWN,
        THEFT_OR_PILFERAGE,
        TRAFFIC_VIOLATION_FINE,
        INTERIOR_DAMAGE
    }

    public enum IncidentSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum IncidentStatus {
        REPORTED,
        INVESTIGATING,
        INSURANCE_CLAIM_FILED,
        RESOLVED,
        CLOSED
    }
}
