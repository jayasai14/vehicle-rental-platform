package com.vehiclerental.modules.booking.repository;

import com.vehiclerental.modules.booking.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingCode(String bookingCode);

    Optional<Booking> findByHoldId(Long holdId);

    Page<Booking> findByCustomerUserIdOrderByCreatedAtDesc(Long customerUserId, Pageable pageable);

    Page<Booking> findByManagerPartnerIdOrderByCreatedAtDesc(Long managerPartnerId, Pageable pageable);

    List<Booking> findByVehicleId(Long vehicleId);

    @Query("SELECT b FROM Booking b " +
           "WHERE b.vehicle.id = :vehicleId " +
           "AND b.bookingStatus NOT IN ('CANCELLED', 'NO_SHOW', 'COMPLETED') " +
           "AND b.startTime < :endTime AND b.endTime > :startTime " +
           "AND (:excludeBookingId IS NULL OR b.id <> :excludeBookingId)")
    List<Booking> findOverlappingBookings(
            @Param("vehicleId") Long vehicleId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeBookingId") Long excludeBookingId
    );

    @Query("SELECT b FROM Booking b " +
           "WHERE b.bookingStatus = 'IN_PROGRESS' " +
           "AND b.endTime < :now")
    List<Booking> findOverdueBookings(@Param("now") LocalDateTime now);
}
