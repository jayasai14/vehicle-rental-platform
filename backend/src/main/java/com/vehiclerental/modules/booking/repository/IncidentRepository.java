package com.vehiclerental.modules.booking.repository;

import com.vehiclerental.modules.booking.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {
    Optional<Incident> findByIncidentReference(String incidentReference);
    List<Incident> findByBookingId(Long bookingId);
    List<Incident> findByVehicleId(Long vehicleId);
    List<Incident> findByStatus(Incident.IncidentStatus status);
}
