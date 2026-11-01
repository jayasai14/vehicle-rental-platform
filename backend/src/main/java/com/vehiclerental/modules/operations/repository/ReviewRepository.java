package com.vehiclerental.modules.operations.repository;

import com.vehiclerental.modules.operations.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByBookingId(Long bookingId);
    Page<Review> findByVehicleIdAndIsPublishedTrueOrderByCreatedAtDesc(Long vehicleId, Pageable pageable);
}
