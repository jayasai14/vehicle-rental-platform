package com.vehiclerental.modules.vehicle.repository;

import com.vehiclerental.modules.vehicle.entity.VehicleImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleImageRepository extends JpaRepository<VehicleImage, Long> {
    List<VehicleImage> findByVehicleIdOrderByDisplayOrderAsc(Long vehicleId);
}
