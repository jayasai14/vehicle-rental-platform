package com.vehiclerental.modules.vehicle.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehicle_features")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "feature_name", nullable = false, length = 100)
    private String featureName;

    @Enumerated(EnumType.STRING)
    @Column(name = "feature_category", nullable = false, length = 30)
    private FeatureCategory featureCategory;

    public enum FeatureCategory {
        SAFETY,
        COMFORT,
        ENTERTAINMENT,
        PERFORMANCE,
        CONVENIENCE
    }
}
