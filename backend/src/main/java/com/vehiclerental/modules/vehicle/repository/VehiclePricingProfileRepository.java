package com.vehiclerental.modules.vehicle.repository;

import com.vehiclerental.modules.vehicle.entity.VehiclePricingProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehiclePricingProfileRepository extends JpaRepository<VehiclePricingProfile, Long> {
    Optional<VehiclePricingProfile> findByVehicleId(Long vehicleId);
}
