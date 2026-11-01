package com.vehiclerental.modules.booking.repository;

import com.vehiclerental.modules.booking.entity.BookingExtension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingExtensionRepository extends JpaRepository<BookingExtension, Long> {
    List<BookingExtension> findByBookingId(Long bookingId);
}
