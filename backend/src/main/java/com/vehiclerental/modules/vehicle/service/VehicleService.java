package com.vehiclerental.modules.vehicle.service;

import com.vehiclerental.common.exception.BadRequestException;
import com.vehiclerental.common.exception.ForbiddenException;
import com.vehiclerental.common.exception.ResourceNotFoundException;
import com.vehiclerental.modules.booking.entity.Booking;
import com.vehiclerental.modules.booking.entity.BookingHold;
import com.vehiclerental.modules.booking.repository.BookingHoldRepository;
import com.vehiclerental.modules.booking.repository.BookingRepository;
import com.vehiclerental.modules.location.entity.PartnerHub;
import com.vehiclerental.modules.location.repository.PartnerHubRepository;
import com.vehiclerental.modules.partner.entity.PartnerProfile;
import com.vehiclerental.modules.partner.repository.PartnerProfileRepository;
import com.vehiclerental.modules.partner.service.PartnerService;
import com.vehiclerental.modules.user.entity.User;
import com.vehiclerental.modules.user.repository.UserRepository;
import com.vehiclerental.modules.vehicle.dto.VehicleDto;
import com.vehiclerental.modules.vehicle.entity.*;
import com.vehiclerental.modules.vehicle.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehiclePricingProfileRepository pricingRepository;
    private final VehicleImageRepository imageRepository;
    private final VehicleFeatureRepository featureRepository;
    private final VehicleMaintenanceBlockRepository maintenanceRepository;
    private final PartnerProfileRepository partnerProfileRepository;
    private final PartnerService partnerService;
    private final PartnerHubRepository partnerHubRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final BookingHoldRepository holdRepository;

    @Transactional(readOnly = true)
    public Page<VehicleDto.VehicleCardDto> searchVehicles(
            Long cityId,
            Long hubId,
            String categoryStr,
            LocalDateTime startTime,
            LocalDateTime endTime,
            BigDecimal maxDailyRate,
            String transmissionStr,
            Pageable pageable
    ) {
        Vehicle.VehicleCategory category = null;
        if (categoryStr != null && !categoryStr.isBlank()) {
            category = Vehicle.VehicleCategory.valueOf(categoryStr.toUpperCase());
        }

        Page<Vehicle> rawPage = vehicleRepository.searchPublishedVehicles(category, cityId, hubId, pageable);

        List<VehicleDto.VehicleCardDto> filtered = rawPage.getContent().stream()
                .filter(v -> {
                    if (transmissionStr != null && !transmissionStr.isBlank()) {
                        if (!v.getTransmission().name().equalsIgnoreCase(transmissionStr)) return false;
                    }
                    VehiclePricingProfile pricing = pricingRepository.findByVehicleId(v.getId()).orElse(null);
                    if (maxDailyRate != null && pricing != null) {
                        if (pricing.getDailyRate().compareTo(maxDailyRate) > 0) return false;
                    }
                    if (startTime != null && endTime != null) {
                        return isVehicleAvailable(v.getId(), startTime, endTime, null, null);
                    }
                    return true;
                })
                .map(this::mapToCardDto)
                .collect(Collectors.toList());

        return new PageImpl<>(filtered, pageable, rawPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public VehicleDto.VehicleDetailDto getVehicleDetail(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + vehicleId));
        return mapToDetailDto(vehicle);
    }

    @Transactional(readOnly = true)
    public List<VehicleDto.VehicleCardDto> getPartnerFleet(Long userId) {
        PartnerProfile partner = partnerService.validateVerifiedPartner(userId);
        return vehicleRepository.findByManagerPartnerId(partner.getId()).stream()
                .map(this::mapToCardDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public VehicleDto.VehicleDetailDto createVehicle(Long userId, VehicleDto.VehicleCreateRequest request) {
        PartnerProfile partner = partnerService.validateVerifiedPartner(userId);

        if (vehicleRepository.findByRegistrationNumber(request.getRegistrationNumber()).isPresent()) {
            throw new BadRequestException("Vehicle with registration " + request.getRegistrationNumber() + " already exists");
        }

        PartnerHub hub = null;
        if (request.getAssignedHubId() != null) {
            hub = partnerHubRepository.findById(request.getAssignedHubId()).orElse(null);
        }

        Vehicle vehicle = Vehicle.builder()
                .registrationNumber(request.getRegistrationNumber())
                .vinChassisNumber(request.getVinChassisNumber())
                .engineNumber(request.getEngineNumber())
                .make(request.getMake())
                .model(request.getModel())
                .yearOfManufacture(request.getYearOfManufacture())
                .category(Vehicle.VehicleCategory.valueOf(request.getCategory().toUpperCase()))
                .transmission(request.getTransmission() != null ? Vehicle.TransmissionType.valueOf(request.getTransmission().toUpperCase()) : Vehicle.TransmissionType.MANUAL)
                .fuelType(request.getFuelType() != null ? Vehicle.FuelType.valueOf(request.getFuelType().toUpperCase()) : Vehicle.FuelType.PETROL)
                .seatingCapacity(request.getSeatingCapacity() != null ? request.getSeatingCapacity() : 4)
                .odometerReadingKm(request.getOdometerReadingKm() != null ? request.getOdometerReadingKm() : 0)
                .currentFuelOrBatteryLevelPercent(100)
                .ownershipType(Vehicle.OwnershipType.PARTNER_BUSINESS_OWNER)
                .legalOwnerPartner(partner)
                .managerPartner(partner)
                .assignedHub(hub)
                .currentCustodyStatus(Vehicle.CustodyStatus.PARTNER_CUSTODY)
                .listingStatus(Vehicle.ListingStatus.PUBLISHED)
                .turnaroundBufferMinutes(60)
                .build();
        vehicle = vehicleRepository.save(vehicle);

        VehiclePricingProfile pricing = VehiclePricingProfile.builder()
                .vehicle(vehicle)
                .hourlyRate(request.getHourlyRate())
                .dailyRate(request.getDailyRate())
                .weeklyRate(request.getWeeklyRate())
                .securityDepositAmount(request.getSecurityDepositAmount())
                .freeKmPerDay(request.getFreeKmPerDay() != null ? request.getFreeKmPerDay() : 300)
                .extraKmCharge(request.getExtraKmCharge() != null ? request.getExtraKmCharge() : new BigDecimal("10.00"))
                .lateReturnHourlyPenalty(new BigDecimal("200.00"))
                .isActive(true)
                .build();
        pricingRepository.save(pricing);

        if (request.getPrimaryImageUrl() != null && !request.getPrimaryImageUrl().isBlank()) {
            VehicleImage image = VehicleImage.builder()
                    .vehicle(vehicle)
                    .storageObjectKey(request.getPrimaryImageUrl())
                    .isPrimary(true)
                    .displayOrder(0)
                    .build();
            imageRepository.save(image);
        }

        if (request.getFeatures() != null) {
            for (String feat : request.getFeatures()) {
                VehicleFeature vf = VehicleFeature.builder()
                        .vehicle(vehicle)
                        .featureName(feat)
                        .featureCategory(VehicleFeature.FeatureCategory.CONVENIENCE)
                        .build();
                featureRepository.save(vf);
            }
        }

        return mapToDetailDto(vehicle);
    }

    @Transactional
    public VehicleDto.VehicleDetailDto updateVehicle(Long userId, Long vehicleId, VehicleDto.VehicleCreateRequest request) {
        PartnerProfile partner = partnerService.validateVerifiedPartner(userId);
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + vehicleId));

        if (!vehicle.getManagerPartner().getId().equals(partner.getId())) {
            throw new ForbiddenException("Vehicle does not belong to your partner profile");
        }

        vehicle.setMake(request.getMake());
        vehicle.setModel(request.getModel());
        vehicle.setYearOfManufacture(request.getYearOfManufacture());
        if (request.getCategory() != null) {
            vehicle.setCategory(Vehicle.VehicleCategory.valueOf(request.getCategory().toUpperCase()));
        }
        if (request.getTransmission() != null) {
            vehicle.setTransmission(Vehicle.TransmissionType.valueOf(request.getTransmission().toUpperCase()));
        }
        if (request.getFuelType() != null) {
            vehicle.setFuelType(Vehicle.FuelType.valueOf(request.getFuelType().toUpperCase()));
        }
        if (request.getSeatingCapacity() != null) {
            vehicle.setSeatingCapacity(request.getSeatingCapacity());
        }
        if (request.getOdometerReadingKm() != null) {
            vehicle.setOdometerReadingKm(request.getOdometerReadingKm());
        }
        if (request.getAssignedHubId() != null) {
            PartnerHub hub = partnerHubRepository.findById(request.getAssignedHubId()).orElse(null);
            vehicle.setAssignedHub(hub);
        }
        vehicle = vehicleRepository.save(vehicle);

        VehiclePricingProfile pricing = pricingRepository.findByVehicleId(vehicle.getId()).orElse(null);
        if (pricing == null) {
            pricing = VehiclePricingProfile.builder().vehicle(vehicle).build();
        }
        pricing.setHourlyRate(request.getHourlyRate());
        pricing.setDailyRate(request.getDailyRate());
        pricing.setWeeklyRate(request.getWeeklyRate());
        pricing.setSecurityDepositAmount(request.getSecurityDepositAmount());
        if (request.getFreeKmPerDay() != null) pricing.setFreeKmPerDay(request.getFreeKmPerDay());
        if (request.getExtraKmCharge() != null) pricing.setExtraKmCharge(request.getExtraKmCharge());
        pricingRepository.save(pricing);

        if (request.getPrimaryImageUrl() != null && !request.getPrimaryImageUrl().isBlank()) {
            List<VehicleImage> existingImages = imageRepository.findByVehicleIdOrderByDisplayOrderAsc(vehicle.getId());
            boolean exists = existingImages.stream().anyMatch(img -> request.getPrimaryImageUrl().equals(img.getStorageObjectKey()));
            if (!exists) {
                VehicleImage image = VehicleImage.builder()
                        .vehicle(vehicle)
                        .storageObjectKey(request.getPrimaryImageUrl())
                        .isPrimary(true)
                        .displayOrder(0)
                        .build();
                imageRepository.save(image);
            }
        }

        return mapToDetailDto(vehicle);
    }

    @Transactional
    public VehicleDto.VehicleDetailDto updateVehicleStatus(Long userId, Long vehicleId, String statusStr) {
        PartnerProfile partner = partnerService.validateVerifiedPartner(userId);
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + vehicleId));

        if (!vehicle.getManagerPartner().getId().equals(partner.getId())) {
            throw new ForbiddenException("Vehicle does not belong to your partner profile");
        }

        if (statusStr != null && !statusStr.isBlank()) {
            try {
                vehicle.setListingStatus(Vehicle.ListingStatus.valueOf(statusStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                try {
                    vehicle.setCurrentCustodyStatus(Vehicle.CustodyStatus.valueOf(statusStr.toUpperCase()));
                } catch (IllegalArgumentException ex) {
                    throw new BadRequestException("Invalid status value: " + statusStr);
                }
            }
            vehicle = vehicleRepository.save(vehicle);
        }

        return mapToDetailDto(vehicle);
    }

    @Transactional
    public VehicleDto.MaintenanceBlockDto createMaintenanceBlock(Long userId, Long vehicleId, VehicleDto.CreateMaintenanceBlockRequest request) {
        PartnerProfile partner = partnerService.validateVerifiedPartner(userId);
        User user = userRepository.findById(userId).orElseThrow();
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        if (!vehicle.getManagerPartner().getId().equals(partner.getId())) {
            throw new ForbiddenException("Vehicle does not belong to your partner profile");
        }

        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new BadRequestException("End time must be after start time");
        }

        VehicleMaintenanceBlock block = VehicleMaintenanceBlock.builder()
                .vehicle(vehicle)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .reason(request.getReason())
                .blockedByUser(user)
                .build();
        block = maintenanceRepository.save(block);

        return VehicleDto.MaintenanceBlockDto.builder()
                .id(block.getId())
                .vehicleId(vehicle.getId())
                .startTime(block.getStartTime())
                .endTime(block.getEndTime())
                .reason(block.getReason())
                .build();
    }

    @Transactional(readOnly = true)
    public boolean isVehicleAvailable(
            Long vehicleId,
            LocalDateTime start,
            LocalDateTime end,
            Long excludeBookingId,
            Long excludeHoldId
    ) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId).orElse(null);
        if (vehicle == null || vehicle.getListingStatus() != Vehicle.ListingStatus.PUBLISHED) {
            return false;
        }

        // Apply buffer minutes before and after
        int bufferMinutes = vehicle.getTurnaroundBufferMinutes() != null ? vehicle.getTurnaroundBufferMinutes() : 60;
        LocalDateTime bufferedStart = start.minusMinutes(bufferMinutes);
        LocalDateTime bufferedEnd = end.plusMinutes(bufferMinutes);

        // 1. Check maintenance blocks
        List<VehicleMaintenanceBlock> maintenance = maintenanceRepository.findOverlappingBlocks(vehicleId, bufferedStart, bufferedEnd);
        if (!maintenance.isEmpty()) return false;

        // 2. Check active holds
        List<BookingHold> holds = holdRepository.findOverlappingActiveHolds(vehicleId, bufferedStart, bufferedEnd, LocalDateTime.now(), excludeHoldId);
        if (!holds.isEmpty()) return false;

        // 3. Check existing confirmed/in-progress bookings
        List<Booking> bookings = bookingRepository.findOverlappingBookings(vehicleId, bufferedStart, bufferedEnd, excludeBookingId);
        return bookings.isEmpty();
    }

    private VehicleDto.VehicleCardDto mapToCardDto(Vehicle v) {
        VehiclePricingProfile pricing = pricingRepository.findByVehicleId(v.getId()).orElse(null);
        List<VehicleImage> images = imageRepository.findByVehicleIdOrderByDisplayOrderAsc(v.getId());
        String primaryImage = images.stream().filter(VehicleImage::getIsPrimary).findFirst()
                .map(VehicleImage::getStorageObjectKey)
                .orElse(images.isEmpty() ? null : images.get(0).getStorageObjectKey());

        return VehicleDto.VehicleCardDto.builder()
                .id(v.getId())
                .make(v.getMake())
                .model(v.getModel())
                .yearOfManufacture(v.getYearOfManufacture())
                .category(v.getCategory().name())
                .transmission(v.getTransmission().name())
                .fuelType(v.getFuelType().name())
                .seatingCapacity(v.getSeatingCapacity())
                .primaryImageUrl(primaryImage)
                .hourlyRate(pricing != null ? pricing.getHourlyRate() : BigDecimal.ZERO)
                .dailyRate(pricing != null ? pricing.getDailyRate() : BigDecimal.ZERO)
                .securityDepositAmount(pricing != null ? pricing.getSecurityDepositAmount() : BigDecimal.ZERO)
                .partnerBusinessName(v.getManagerPartner() != null ? v.getManagerPartner().getBusinessName() : "")
                .cityName(v.getAssignedHub() != null && v.getAssignedHub().getCity() != null ? v.getAssignedHub().getCity().getName() : "Vijayawada")
                .hubName(v.getAssignedHub() != null ? v.getAssignedHub().getName() : "")
                .custodyStatus(v.getCurrentCustodyStatus().name())
                .listingStatus(v.getListingStatus().name())
                .build();
    }

    private VehicleDto.VehicleDetailDto mapToDetailDto(Vehicle v) {
        VehiclePricingProfile pricing = pricingRepository.findByVehicleId(v.getId()).orElse(null);
        List<VehicleImage> images = imageRepository.findByVehicleIdOrderByDisplayOrderAsc(v.getId());
        List<VehicleFeature> features = featureRepository.findByVehicleId(v.getId());

        return VehicleDto.VehicleDetailDto.builder()
                .id(v.getId())
                .registrationNumber(v.getRegistrationNumber())
                .vinChassisNumber(v.getVinChassisNumber())
                .engineNumber(v.getEngineNumber())
                .make(v.getMake())
                .model(v.getModel())
                .yearOfManufacture(v.getYearOfManufacture())
                .category(v.getCategory().name())
                .transmission(v.getTransmission().name())
                .fuelType(v.getFuelType().name())
                .seatingCapacity(v.getSeatingCapacity())
                .odometerReadingKm(v.getOdometerReadingKm())
                .fuelOrBatteryLevelPercent(v.getCurrentFuelOrBatteryLevelPercent())
                .ownershipType(v.getOwnershipType().name())
                .managerPartnerId(v.getManagerPartner().getId())
                .managerPartnerBusinessName(v.getManagerPartner().getBusinessName())
                .assignedHubId(v.getAssignedHub() != null ? v.getAssignedHub().getId() : null)
                .assignedHubName(v.getAssignedHub() != null ? v.getAssignedHub().getName() : null)
                .cityName(v.getAssignedHub() != null && v.getAssignedHub().getCity() != null ? v.getAssignedHub().getCity().getName() : "Vijayawada")
                .currentCustodyStatus(v.getCurrentCustodyStatus().name())
                .listingStatus(v.getListingStatus().name())
                .turnaroundBufferMinutes(v.getTurnaroundBufferMinutes())
                .pricing(pricing != null ? VehicleDto.VehiclePricingDto.builder()
                        .hourlyRate(pricing.getHourlyRate())
                        .dailyRate(pricing.getDailyRate())
                        .weeklyRate(pricing.getWeeklyRate())
                        .securityDepositAmount(pricing.getSecurityDepositAmount())
                        .freeKmPerDay(pricing.getFreeKmPerDay())
                        .extraKmCharge(pricing.getExtraKmCharge())
                        .lateReturnHourlyPenalty(pricing.getLateReturnHourlyPenalty())
                        .build() : null)
                .images(images.stream().map(img -> VehicleDto.VehicleImageDto.builder()
                        .id(img.getId())
                        .storageObjectKey(img.getStorageObjectKey())
                        .caption(img.getCaption())
                        .isPrimary(img.getIsPrimary())
                        .displayOrder(img.getDisplayOrder())
                        .build()).collect(Collectors.toList()))
                .features(features.stream().map(VehicleFeature::getFeatureName).collect(Collectors.toList()))
                .build();
    }
}
