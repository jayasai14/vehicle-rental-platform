package com.vehiclerental.modules.custody.repository;

import com.vehiclerental.modules.custody.entity.VehicleCustodyEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleCustodyEventRepository extends JpaRepository<VehicleCustodyEvent, Long> {
    List<VehicleCustodyEvent> findByVehicleIdOrderByEventTimestampDesc(Long vehicleId);
    List<VehicleCustodyEvent> findByBookingId(Long bookingId);
}
