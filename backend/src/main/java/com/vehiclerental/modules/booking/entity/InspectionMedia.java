package com.vehiclerental.modules.booking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inspection_media")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspectionMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_id", nullable = false)
    private Inspection inspection;

    @Enumerated(EnumType.STRING)
    @Column(name = "view_angle", nullable = false, length = 30)
    private ViewAngle viewAngle;

    @Column(name = "storage_object_key", nullable = false)
    private String storageObjectKey;

    @Column(length = 255)
    private String caption;

    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;

    @PrePersist
    protected void onCreate() {
        if (capturedAt == null) capturedAt = LocalDateTime.now();
    }

    public enum ViewAngle {
        FRONT,
        REAR,
        LEFT_SIDE,
        RIGHT_SIDE,
        ODOMETER_DASH,
        FUEL_GAUGE,
        INTERIOR_CABIN,
        CLOSEUP_DAMAGE,
        SPARE_TYRE
    }
}
