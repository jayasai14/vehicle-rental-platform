package com.vehiclerental.modules.vehicle.repository;

import com.vehiclerental.modules.vehicle.entity.VehicleFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleFeatureRepository extends JpaRepository<VehicleFeature, Long> {
    List<VehicleFeature> findByVehicleId(Long vehicleId);
}
