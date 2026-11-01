package com.vehiclerental.modules.operations.controller;

import com.vehiclerental.common.response.ApiResponse;
import com.vehiclerental.common.response.PagedResponse;
import com.vehiclerental.common.security.SecurityUtils;
import com.vehiclerental.modules.operations.dto.OperationsDto;
import com.vehiclerental.modules.operations.service.NotificationService;
import com.vehiclerental.modules.operations.service.ReviewService;
import com.vehiclerental.modules.operations.service.SupportTicketService;
import com.vehiclerental.modules.user.entity.User;
import com.vehiclerental.modules.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OperationsController {

    private final SupportTicketService ticketService;
    private final ReviewService reviewService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @PostMapping({"/support/tickets", "/operations/tickets"})
    public ResponseEntity<ApiResponse<OperationsDto.TicketDto>> createTicket(
            @Valid @RequestBody OperationsDto.CreateTicketRequest request
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        OperationsDto.TicketDto ticket = ticketService.createTicket(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(ticket, "Support ticket opened successfully"));
    }

    @GetMapping({"/support/tickets", "/operations/tickets"})
    public ResponseEntity<ApiResponse<PagedResponse<OperationsDto.TicketDto>>> getUserTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        Pageable pageable = PageRequest.of(page, size);
        Page<OperationsDto.TicketDto> result = ticketService.getUserTickets(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(result), "Tickets retrieved"));
    }

    @GetMapping({"/support/tickets/{id}", "/operations/tickets/{id}"})
    public ResponseEntity<ApiResponse<OperationsDto.TicketDto>> getTicket(@PathVariable Long id) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElse(null);
        Long userId = user != null ? user.getId() : null;
        return ResponseEntity.ok(ApiResponse.success(ticketService.getTicketById(userId, id), "Ticket retrieved"));
    }

    @PostMapping({"/support/tickets/{id}/messages", "/operations/tickets/{id}/messages"})
    public ResponseEntity<ApiResponse<OperationsDto.TicketMessageDto>> sendMessage(
            @PathVariable Long id,
            @Valid @RequestBody OperationsDto.SendMessageRequest request
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        OperationsDto.TicketMessageDto msg = ticketService.sendMessage(user.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(msg, "Message sent"));
    }

    @GetMapping({"/support/tickets/{id}/messages", "/operations/tickets/{id}/messages"})
    public ResponseEntity<ApiResponse<java.util.List<OperationsDto.TicketMessageDto>>> getTicketMessages(@PathVariable Long id) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElse(null);
        Long userId = user != null ? user.getId() : null;
        return ResponseEntity.ok(ApiResponse.success(ticketService.getTicketMessages(userId, id), "Messages retrieved"));
    }

    @PostMapping({"/reviews", "/operations/reviews/booking/{bookingId}"})
    public ResponseEntity<ApiResponse<OperationsDto.ReviewDto>> submitReview(
            @PathVariable(required = false) Long bookingId,
            @Valid @RequestBody OperationsDto.CreateReviewRequest request
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        if (bookingId != null && request.getBookingId() == null) {
            request.setBookingId(bookingId);
        }
        OperationsDto.ReviewDto review = reviewService.createReview(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(review, "Review submitted successfully"));
    }

    @GetMapping({"/reviews/vehicle/{vehicleId}", "/operations/reviews/vehicle/{vehicleId}"})
    public ResponseEntity<ApiResponse<PagedResponse<OperationsDto.ReviewDto>>> getVehicleReviews(
            @PathVariable Long vehicleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<OperationsDto.ReviewDto> result = reviewService.getVehicleReviews(vehicleId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(result), "Reviews retrieved"));
    }

    @GetMapping({"/notifications", "/operations/notifications"})
    public ResponseEntity<ApiResponse<PagedResponse<OperationsDto.NotificationDto>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        Pageable pageable = PageRequest.of(page, size);
        Page<OperationsDto.NotificationDto> result = notificationService.getUserNotifications(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(result), "Notifications retrieved"));
    }

    @RequestMapping(value = {"/notifications/{id}/read", "/operations/notifications/{id}/read"}, method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<ApiResponse<Void>> markNotificationRead(@PathVariable Long id) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        notificationService.markAsRead(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Notification marked as read"));
    }
}
