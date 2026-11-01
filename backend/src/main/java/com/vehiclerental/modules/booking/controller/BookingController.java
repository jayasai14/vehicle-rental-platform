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

    @PostMapping({"/booking-holds", "/bookings/holds"})
    public ResponseEntity<ApiResponse<BookingDto.HoldResponseDto>> createHold(
            @Valid @RequestBody BookingDto.CreateHoldRequest request
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        BookingDto.HoldResponseDto response = bookingService.createBookingHold(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Booking hold created successfully with 15-minute validity"));
    }

    @GetMapping({"/booking-holds/{id}", "/bookings/holds/{id}"})
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
            @RequestBody(required = false) java.util.Map<String, String> body,
            @RequestParam(required = false) String reason
    ) {
        String finalReason = reason;
        if (finalReason == null && body != null) {
            finalReason = body.getOrDefault("cancellationReason", body.get("reason"));
        }
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        BookingDto.BookingResponseDto booking = bookingService.cancelBooking(user.getId(), id, finalReason);
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

    @PostMapping({"/partners/bookings/{bookingId}/handover", "/bookings/{bookingId}/handover"})
    @PreAuthorize("hasAnyRole('RENTAL_PARTNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<BookingDto.BookingResponseDto>> handoverVehicle(
            @PathVariable Long bookingId,
            @RequestBody(required = false) java.util.Map<String, Object> body
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();

        Integer odometer = body != null && body.get("odometerReading") != null ? ((Number) body.get("odometerReading")).intValue() : 24500;
        Integer fuel = body != null && body.get("fuelPercent") != null ? ((Number) body.get("fuelPercent")).intValue() : 100;
        String notes = body != null && body.get("conditionNotes") != null ? body.get("conditionNotes").toString() : "Vehicle delivered in pristine condition";

        BookingDto.SubmitInspectionRequest req = BookingDto.SubmitInspectionRequest.builder()
                .bookingId(bookingId)
                .inspectionType("PRE_RENTAL_HANDOVER")
                .odometerReadingKm(odometer)
                .fuelOrBatteryLevelPercent(fuel)
                .exteriorConditionNotes(notes)
                .customerSignatureConfirmed(true)
                .partnerSignatureConfirmed(true)
                .build();
        bookingService.conductInspection(user.getId(), req);
        return ResponseEntity.ok(ApiResponse.success(bookingService.getBookingById(bookingId), "Pre-rental handover completed"));
    }

    @PostMapping({"/partners/bookings/{bookingId}/return", "/bookings/{bookingId}/return"})
    @PreAuthorize("hasAnyRole('RENTAL_PARTNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<BookingDto.BookingResponseDto>> returnVehicle(
            @PathVariable Long bookingId,
            @RequestBody(required = false) java.util.Map<String, Object> body
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();

        Integer odometer = body != null && body.get("odometerReading") != null ? ((Number) body.get("odometerReading")).intValue() : 24820;
        Integer fuel = body != null && body.get("fuelPercent") != null ? ((Number) body.get("fuelPercent")).intValue() : 95;
        String notes = body != null && body.get("conditionNotes") != null ? body.get("conditionNotes").toString() : "Vehicle returned cleanly";

        BookingDto.SubmitInspectionRequest req = BookingDto.SubmitInspectionRequest.builder()
                .bookingId(bookingId)
                .inspectionType("POST_RENTAL_RETURN")
                .odometerReadingKm(odometer)
                .fuelOrBatteryLevelPercent(fuel)
                .exteriorConditionNotes(notes)
                .customerSignatureConfirmed(true)
                .partnerSignatureConfirmed(true)
                .build();
        bookingService.conductInspection(user.getId(), req);
        return ResponseEntity.ok(ApiResponse.success(bookingService.getBookingById(bookingId), "Post-rental return completed"));
    }

    @GetMapping("/bookings/my")
    public ResponseEntity<ApiResponse<java.util.List<BookingDto.BookingResponseDto>>> getMyBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        Pageable pageable = PageRequest.of(page, size);
        Page<BookingDto.BookingResponseDto> result = bookingService.getCustomerBookings(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), "Customer bookings retrieved"));
    }

    @GetMapping("/bookings/partner")
    @PreAuthorize("hasAnyRole('RENTAL_PARTNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<java.util.List<BookingDto.BookingResponseDto>>> getPartnerBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        Pageable pageable = PageRequest.of(page, size);
        Page<BookingDto.BookingResponseDto> result = bookingService.getPartnerBookings(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), "Partner bookings retrieved"));
    }

    @GetMapping("/bookings/{id}")
    public ResponseEntity<ApiResponse<BookingDto.BookingResponseDto>> getBookingById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getBookingById(id), "Booking retrieved"));
    }
}
