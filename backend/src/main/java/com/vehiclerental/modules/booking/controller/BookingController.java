package com.vehiclerental.modules.booking.controller;

import com.vehiclerental.common.response.ApiResponse;
import com.vehiclerental.common.response.PagedResponse;
import com.vehiclerental.common.security.SecurityUtils;
import com.vehiclerental.modules.booking.dto.BookingDto;
import com.vehiclerental.modules.booking.service.BookingService;
import com.vehiclerental.modules.user.entity.User;
import com.vehiclerental.modules.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    @PostMapping("/booking-holds")
    public ResponseEntity<ApiResponse<BookingDto.HoldResponseDto>> createHold(
            @Valid @RequestBody BookingDto.CreateHoldRequest request
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        BookingDto.HoldResponseDto response = bookingService.createBookingHold(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Booking hold created successfully with 15-minute validity"));
    }

    @GetMapping("/booking-holds/{id}")
    public ResponseEntity<ApiResponse<BookingDto.HoldResponseDto>> getHold(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getHoldById(id), "Hold retrieved"));
    }

    @PostMapping("/bookings/confirm/{holdId}")
    public ResponseEntity<ApiResponse<BookingDto.BookingResponseDto>> confirmBooking(@PathVariable Long holdId) {
        BookingDto.BookingResponseDto booking = bookingService.confirmBookingFromHold(holdId);
        return ResponseEntity.ok(ApiResponse.success(booking, "Booking confirmed successfully"));
    }

    @PostMapping("/bookings/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingDto.BookingResponseDto>> cancelBooking(
            @PathVariable Long id,
            @RequestParam(required = false) String reason
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        BookingDto.BookingResponseDto booking = bookingService.cancelBooking(user.getId(), id, reason);
        return ResponseEntity.ok(ApiResponse.success(booking, "Booking cancelled successfully"));
    }

    @PostMapping("/bookings/inspections")
    @PreAuthorize("hasAnyRole('RENTAL_PARTNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<BookingDto.InspectionResponseDto>> conductInspection(
            @Valid @RequestBody BookingDto.SubmitInspectionRequest request
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        BookingDto.InspectionResponseDto resp = bookingService.conductInspection(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(resp, "Inspection recorded and custody transitioned"));
    }

    @GetMapping("/bookings/my")
    public ResponseEntity<ApiResponse<PagedResponse<BookingDto.BookingResponseDto>>> getMyBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        Pageable pageable = PageRequest.of(page, size);
        Page<BookingDto.BookingResponseDto> result = bookingService.getCustomerBookings(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(result), "Customer bookings retrieved"));
    }

    @GetMapping("/bookings/partner")
    @PreAuthorize("hasAnyRole('RENTAL_PARTNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<BookingDto.BookingResponseDto>>> getPartnerBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        Pageable pageable = PageRequest.of(page, size);
        Page<BookingDto.BookingResponseDto> result = bookingService.getPartnerBookings(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(result), "Partner bookings retrieved"));
    }

    @GetMapping("/bookings/{id}")
    public ResponseEntity<ApiResponse<BookingDto.BookingResponseDto>> getBookingById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getBookingById(id), "Booking retrieved"));
    }
}
