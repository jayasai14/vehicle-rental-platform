package com.vehiclerental.modules.vehicle.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "storage_object_key", nullable = false)
    private String storageObjectKey;

    @Column(length = 100)
    private String caption;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        if (uploadedAt == null) uploadedAt = LocalDateTime.now();
        if (isPrimary == null) isPrimary = false;
        if (displayOrder == null) displayOrder = 0;
    }
}
