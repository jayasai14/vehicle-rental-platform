package com.vehiclerental.modules.vehicle.repository;

import com.vehiclerental.modules.vehicle.entity.VehicleDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, Long> {
    List<VehicleDocument> findByVehicleId(Long vehicleId);
    List<VehicleDocument> findByExpiryDateBefore(LocalDate date);
}
