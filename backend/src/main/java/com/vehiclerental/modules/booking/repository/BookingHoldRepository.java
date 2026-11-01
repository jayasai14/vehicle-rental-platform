package com.vehiclerental.modules.booking.repository;

import com.vehiclerental.modules.booking.entity.BookingHold;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingHoldRepository extends JpaRepository<BookingHold, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM BookingHold h WHERE h.id = :id")
    Optional<BookingHold> findByIdWithLock(@Param("id") Long id);

    Optional<BookingHold> findByHoldReference(String holdReference);

    List<BookingHold> findByCustomerUserId(Long customerUserId);

    @Query("SELECT h FROM BookingHold h " +
           "WHERE h.vehicle.id = :vehicleId " +
           "AND h.status IN ('ACTIVE', 'VERIFICATION_PENDING') " +
           "AND h.expiresAt > :now " +
           "AND h.startTime < :endTime AND h.endTime > :startTime " +
           "AND (:excludeHoldId IS NULL OR h.id <> :excludeHoldId)")
    List<BookingHold> findOverlappingActiveHolds(
            @Param("vehicleId") Long vehicleId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("now") LocalDateTime now,
            @Param("excludeHoldId") Long excludeHoldId
    );

    @Query("SELECT h FROM BookingHold h " +
           "WHERE h.status IN ('ACTIVE', 'VERIFICATION_PENDING') " +
           "AND h.expiresAt <= :now")
    List<BookingHold> findExpiredHolds(@Param("now") LocalDateTime now);
}
