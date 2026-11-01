package com.vehiclerental.modules.vehicle.repository;

import com.vehiclerental.modules.vehicle.entity.Vehicle;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Vehicle v WHERE v.id = :id")
    Optional<Vehicle> findByIdWithLock(@Param("id") Long id);

    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);

    List<Vehicle> findByManagerPartnerId(Long managerPartnerId);

    List<Vehicle> findByLegalOwnerUserId(Long legalOwnerUserId);

    Page<Vehicle> findByListingStatus(Vehicle.ListingStatus status, Pageable pageable);

    @Query("SELECT v FROM Vehicle v " +
           "WHERE v.listingStatus = 'PUBLISHED' " +
           "AND (:category IS NULL OR v.category = :category) " +
           "AND (:cityId IS NULL OR v.assignedHub.city.id = :cityId) " +
           "AND (:hubId IS NULL OR v.assignedHub.id = :hubId)")
    Page<Vehicle> searchPublishedVehicles(
            @Param("category") Vehicle.VehicleCategory category,
            @Param("cityId") Long cityId,
            @Param("hubId") Long hubId,
            Pageable pageable
    );
}
