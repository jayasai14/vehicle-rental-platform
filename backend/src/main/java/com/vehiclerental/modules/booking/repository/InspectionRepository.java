package com.vehiclerental.modules.booking.repository;

import com.vehiclerental.modules.booking.entity.Inspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InspectionRepository extends JpaRepository<Inspection, Long> {
    List<Inspection> findByBookingId(Long bookingId);
    List<Inspection> findByVehicleId(Long vehicleId);
}
