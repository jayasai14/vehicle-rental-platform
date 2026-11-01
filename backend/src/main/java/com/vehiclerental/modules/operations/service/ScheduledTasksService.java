package com.vehiclerental.modules.operations.service;

import com.vehiclerental.modules.booking.entity.Booking;
import com.vehiclerental.modules.booking.entity.BookingHold;
import com.vehiclerental.modules.booking.repository.BookingHoldRepository;
import com.vehiclerental.modules.booking.repository.BookingRepository;
import com.vehiclerental.modules.operations.entity.OutboxEvent;
import com.vehiclerental.modules.operations.repository.OutboxEventRepository;
import com.vehiclerental.modules.payment.entity.RentalPaymentDemand;
import com.vehiclerental.modules.payment.repository.RentalPaymentDemandRepository;
import com.vehiclerental.modules.vehicle.entity.VehicleDocument;
import com.vehiclerental.modules.vehicle.repository.VehicleDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTasksService {

    private final BookingHoldRepository holdRepository;
    private final BookingRepository bookingRepository;
    private final RentalPaymentDemandRepository demandRepository;
    private final VehicleDocumentRepository documentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final NotificationService notificationService;

    /**
     * Process expired booking holds and expire their associated demands if unsatisfied
     */
    @Scheduled(fixedRate = 60000) // Every minute
    @SchedulerLock(name = "processExpiredHolds", lockAtLeastFor = "30s", lockAtMostFor = "55s")
    @Transactional
    public void processExpiredHolds() {
        LocalDateTime now = LocalDateTime.now();
        List<BookingHold> expired = holdRepository.findExpiredHolds(now);
        for (BookingHold hold : expired) {
            log.info("Expiring booking hold: {}", hold.getHoldReference());
            hold.setStatus(BookingHold.HoldStatus.EXPIRED);
            holdRepository.save(hold);

            demandRepository.findByHoldId(hold.getId()).ifPresent(demand -> {
                if (demand.getStatus() == RentalPaymentDemand.DemandStatus.OPEN ||
                    demand.getStatus() == RentalPaymentDemand.DemandStatus.PARTIALLY_SATISFIED) {
                    demand.setStatus(RentalPaymentDemand.DemandStatus.EXPIRED);
                    demandRepository.save(demand);
                }
            });

            notificationService.sendNotification(
                    hold.getCustomerUser().getId(),
                    "Booking Hold Expired",
                    "Your hold for " + hold.getVehicle().getMake() + " " + hold.getVehicle().getModel() + " has expired.",
                    "HOLD_EXPIRED",
                    "booking_holds",
                    String.valueOf(hold.getId())
            );
        }
    }

    /**
     * Check for overdue vehicle returns
     */
    @Scheduled(cron = "0 */15 * * * *") // Every 15 minutes
    @SchedulerLock(name = "checkOverdueBookings", lockAtLeastFor = "1m", lockAtMostFor = "10m")
    @Transactional
    public void checkOverdueBookings() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> overdue = bookingRepository.findOverdueBookings(now);
        for (Booking b : overdue) {
            b.setBookingStatus(Booking.BookingStatus.OVERDUE);
            bookingRepository.save(b);

            log.warn("Booking {} is OVERDUE for return", b.getBookingCode());
            notificationService.sendNotification(
                    b.getCustomerUser().getId(),
                    "Vehicle Return Overdue",
                    "Please return the vehicle immediately to avoid late penalties.",
                    "OVERDUE_WARNING",
                    "bookings",
                    String.valueOf(b.getId())
            );
        }
    }

    /**
     * Daily check for expiring vehicle compliance documents
     */
    @Scheduled(cron = "0 0 8 * * *") // Daily at 8:00 AM
    @SchedulerLock(name = "checkExpiringDocuments", lockAtLeastFor = "5m", lockAtMostFor = "30m")
    @Transactional(readOnly = true)
    public void checkExpiringDocuments() {
        LocalDate warningDate = LocalDate.now().plusDays(15);
        List<VehicleDocument> expiring = documentRepository.findByExpiryDateBefore(warningDate);
        for (VehicleDocument doc : expiring) {
            log.warn("Document {} for vehicle {} expires on {}", doc.getDocumentType(), doc.getVehicle().getRegistrationNumber(), doc.getExpiryDate());
        }
    }

    /**
     * Dispatch pending transactional outbox events
     */
    @Scheduled(fixedRate = 10000) // Every 10 seconds
    @SchedulerLock(name = "dispatchOutboxEvents", lockAtLeastFor = "5s", lockAtMostFor = "9s")
    @Transactional
    public void dispatchOutboxEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                OutboxEvent.OutboxStatus.PENDING,
                PageRequest.of(0, 50)
        );

        for (OutboxEvent evt : pending) {
            try {
                // In local/MVP, dispatching writes to log and marks PROCESSED
                log.info("Processing outbox event: [type={}, aggId={}]", evt.getEventType(), evt.getAggregateId());
                evt.setStatus(OutboxEvent.OutboxStatus.PROCESSED);
                evt.setProcessedAt(LocalDateTime.now());
            } catch (Exception e) {
                log.error("Failed to process outbox event: {}", evt.getId(), e);
                evt.setRetryCount(evt.getRetryCount() + 1);
                evt.setLastError(e.getMessage());
                if (evt.getRetryCount() >= evt.getMaxRetries()) {
                    evt.setStatus(OutboxEvent.OutboxStatus.DEAD_LETTER);
                } else {
                    evt.setStatus(OutboxEvent.OutboxStatus.FAILED);
                }
            }
            outboxEventRepository.save(evt);
        }
    }
}
