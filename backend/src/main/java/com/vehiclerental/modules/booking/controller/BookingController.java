package com.vehiclerental.modules.booking.controller;

import com.vehiclerental.common.exception.BadRequestException;
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
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElse(null);
        Long userId = user != null ? user.getId() : null;
        return ResponseEntity.ok(ApiResponse.success(bookingService.getHoldById(userId, id), "Hold retrieved"));
    }

    @DeleteMapping({"/booking-holds/{id}", "/bookings/holds/{id}"})
    public ResponseEntity<ApiResponse<Void>> cancelHold(@PathVariable Long id) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        bookingService.releaseHold(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Booking hold released successfully"));
    }

    @PostMapping("/bookings/confirm/{holdId}")
    public ResponseEntity<ApiResponse<BookingDto.BookingResponseDto>> confirmBooking(@PathVariable Long holdId) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        BookingDto.BookingResponseDto booking = bookingService.confirmBookingFromHold(user.getId(), holdId);
        return ResponseEntity.ok(ApiResponse.success(booking, "Booking confirmed successfully"));
    }

    @PostMapping("/bookings/{id}/extend")
    public ResponseEntity<ApiResponse<BookingDto.ExtensionResponseDto>> requestExtension(
            @PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, Object> body
    ) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();
        Integer hours = body != null && body.get("requestedHours") != null ? ((Number) body.get("requestedHours")).intValue() : null;
        java.time.LocalDateTime requestedEnd = null;
        if (body != null && body.get("requestedEndTime") != null) {
            requestedEnd = java.time.LocalDateTime.parse(body.get("requestedEndTime").toString());
        }
        BookingDto.ExtensionResponseDto ext = bookingService.extendBooking(user.getId(), id, hours, requestedEnd);
        return ResponseEntity.ok(ApiResponse.success(ext, "Extension requested successfully"));
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
        if (body == null) {
            throw new BadRequestException("Request body with odometer and fuel is required");
        }
        Object odoObj = body.get("odometerReading") != null ? body.get("odometerReading") : body.get("odometerReadingKm");
        Object fuelObj = body.get("fuelPercent") != null ? body.get("fuelPercent") : body.get("fuelOrBatteryLevelPercent");
        if (odoObj == null) {
            throw new BadRequestException("odometerReading is required for handover");
        }
        if (fuelObj == null) {
            throw new BadRequestException("fuelPercent is required for handover");
        }
        int odometer = ((Number) odoObj).intValue();
        int fuel = ((Number) fuelObj).intValue();
        if (odometer < 0) {
            throw new BadRequestException("Odometer reading must be non-negative");
        }
        if (fuel < 0 || fuel > 100) {
            throw new BadRequestException("Fuel level must be between 0 and 100 percent");
        }
        String notes = body.get("conditionNotes") != null ? body.get("conditionNotes").toString() : "Vehicle delivered in pristine condition";

        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();

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
        return ResponseEntity.ok(ApiResponse.success(bookingService.getBookingById(user.getId(), bookingId), "Pre-rental handover completed"));
    }

    @PostMapping({"/partners/bookings/{bookingId}/return", "/bookings/{bookingId}/return"})
    @PreAuthorize("hasAnyRole('RENTAL_PARTNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<BookingDto.BookingResponseDto>> returnVehicle(
            @PathVariable Long bookingId,
            @RequestBody(required = false) java.util.Map<String, Object> body
    ) {
        if (body == null) {
            throw new BadRequestException("Request body with odometer and fuel is required");
        }
        Object odoObj = body.get("odometerReading") != null ? body.get("odometerReading") : body.get("odometerReadingKm");
        Object fuelObj = body.get("fuelPercent") != null ? body.get("fuelPercent") : body.get("fuelOrBatteryLevelPercent");
        if (odoObj == null) {
            throw new BadRequestException("odometerReading is required for return");
        }
        if (fuelObj == null) {
            throw new BadRequestException("fuelPercent is required for return");
        }
        int odometer = ((Number) odoObj).intValue();
        int fuel = ((Number) fuelObj).intValue();
        if (odometer < 0) {
            throw new BadRequestException("Odometer reading must be non-negative");
        }
        if (fuel < 0 || fuel > 100) {
            throw new BadRequestException("Fuel level must be between 0 and 100 percent");
        }
        String notes = body.get("conditionNotes") != null ? body.get("conditionNotes").toString() : "Vehicle returned cleanly";

        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow();

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
        return ResponseEntity.ok(ApiResponse.success(bookingService.getBookingById(user.getId(), bookingId), "Post-rental return completed"));
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
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email).orElse(null);
        Long userId = user != null ? user.getId() : null;
        return ResponseEntity.ok(ApiResponse.success(bookingService.getBookingById(userId, id), "Booking retrieved"));
    }
}
