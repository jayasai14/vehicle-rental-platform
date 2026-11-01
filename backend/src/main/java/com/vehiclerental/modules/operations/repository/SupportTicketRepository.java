package com.vehiclerental.modules.operations.repository;

import com.vehiclerental.modules.operations.entity.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    Optional<SupportTicket> findByTicketReference(String ticketReference);
    Page<SupportTicket> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    List<SupportTicket> findByBookingId(Long bookingId);
    Page<SupportTicket> findByStatusOrderByCreatedAtDesc(SupportTicket.TicketStatus status, Pageable pageable);
}
