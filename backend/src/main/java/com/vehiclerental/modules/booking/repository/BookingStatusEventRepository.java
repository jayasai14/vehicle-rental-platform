package com.vehiclerental.modules.booking.repository;

import com.vehiclerental.modules.booking.entity.BookingStatusEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingStatusEventRepository extends JpaRepository<BookingStatusEvent, Long> {
    List<BookingStatusEvent> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
}
