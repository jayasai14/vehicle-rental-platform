package com.vehiclerental.modules.booking.service;

import com.vehiclerental.modules.booking.entity.Booking;
import com.vehiclerental.modules.booking.repository.BookingHoldRepository;
import com.vehiclerental.modules.booking.repository.BookingRepository;
import com.vehiclerental.modules.vehicle.entity.Vehicle;
import com.vehiclerental.modules.vehicle.entity.VehicleMaintenanceBlock;
import com.vehiclerental.modules.vehicle.repository.VehicleMaintenanceBlockRepository;
import com.vehiclerental.modules.vehicle.repository.VehiclePricingProfileRepository;
import com.vehiclerental.modules.vehicle.repository.VehicleRepository;
import com.vehiclerental.modules.vehicle.service.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityAndBookingTest {

    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private VehiclePricingProfileRepository pricingRepository;
    @Mock
    private VehicleMaintenanceBlockRepository maintenanceRepository;
    @Mock
    private BookingHoldRepository holdRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private com.vehiclerental.modules.partner.service.PartnerService partnerService;
    @Mock
    private com.vehiclerental.modules.vehicle.repository.VehicleImageRepository imageRepository;
    @Mock
    private com.vehiclerental.modules.vehicle.repository.VehicleFeatureRepository featureRepository;
    @Mock
    private com.vehiclerental.modules.partner.repository.PartnerProfileRepository partnerProfileRepository;
    @Mock
    private com.vehiclerental.modules.location.repository.PartnerHubRepository partnerHubRepository;
    @Mock
    private com.vehiclerental.modules.user.repository.UserRepository userRepository;

    private VehicleService vehicleService;

    @BeforeEach
    void setUp() {
        vehicleService = new VehicleService(
                vehicleRepository,
                pricingRepository,
                imageRepository,
                featureRepository,
                maintenanceRepository,
                partnerProfileRepository,
                partnerService,
                partnerHubRepository,
                userRepository,
                bookingRepository,
                holdRepository
        );
    }

    @Test
    @DisplayName("Should detect vehicle unavailable when conflicting confirmed booking exists")
    void testVehicleUnavailableWithOverlappingBooking() {
        Long vehicleId = 10L;
        LocalDateTime start = LocalDateTime.of(2026, 11, 5, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 6, 10, 0);

        Vehicle vehicle = Vehicle.builder()
                .id(vehicleId)
                .listingStatus(Vehicle.ListingStatus.PUBLISHED)
                .turnaroundBufferMinutes(60)
                .build();

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(maintenanceRepository.findOverlappingBlocks(eq(vehicleId), any(), any())).thenReturn(Collections.emptyList());
        when(holdRepository.findOverlappingActiveHolds(eq(vehicleId), any(), any(), any(), any())).thenReturn(Collections.emptyList());

        // Conflicting booking found
        Booking existingBooking = Booking.builder()
                .id(99L)
                .bookingStatus(Booking.BookingStatus.CONFIRMED)
                .build();
        when(bookingRepository.findOverlappingBookings(eq(vehicleId), any(), any(), any()))
                .thenReturn(List.of(existingBooking));

        boolean available = vehicleService.isVehicleAvailable(vehicleId, start, end, null, null);

        assertFalse(available, "Vehicle must not be available when overlapping confirmed booking exists");
    }

    @Test
    @DisplayName("Should detect vehicle available when no conflicts and within published state")
    void testVehicleAvailableWhenNoConflicts() {
        Long vehicleId = 10L;
        LocalDateTime start = LocalDateTime.of(2026, 11, 5, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 6, 10, 0);

        Vehicle vehicle = Vehicle.builder()
                .id(vehicleId)
                .listingStatus(Vehicle.ListingStatus.PUBLISHED)
                .turnaroundBufferMinutes(60)
                .build();

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(maintenanceRepository.findOverlappingBlocks(eq(vehicleId), any(), any())).thenReturn(Collections.emptyList());
        when(holdRepository.findOverlappingActiveHolds(eq(vehicleId), any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(bookingRepository.findOverlappingBookings(eq(vehicleId), any(), any(), any())).thenReturn(Collections.emptyList());

        boolean available = vehicleService.isVehicleAvailable(vehicleId, start, end, null, null);

        assertTrue(available, "Vehicle must be available when no conflicts exist");
    }
}
