package com.vehiclerental.modules.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class VehicleDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VehicleCardDto {
        private Long id;
        private String make;
        private String model;
        private Integer yearOfManufacture;
        private String category;
        private String transmission;
        private String fuelType;
        private Integer seatingCapacity;
        private String primaryImageUrl;
        private BigDecimal hourlyRate;
        private BigDecimal dailyRate;
        private BigDecimal securityDepositAmount;
        private String partnerBusinessName;
        private String cityName;
        private String hubName;
        private String custodyStatus;
        private String listingStatus;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VehicleDetailDto {
        private Long id;
        private String registrationNumber;
        private String vinChassisNumber;
        private String engineNumber;
        private String make;
        private String model;
        private Integer yearOfManufacture;
        private String category;
        private String transmission;
        private String fuelType;
        private Integer seatingCapacity;
        private Integer odometerReadingKm;
        private Integer fuelOrBatteryLevelPercent;
        private String ownershipType;
        private Long managerPartnerId;
        private String managerPartnerBusinessName;
        private Long assignedHubId;
        private String assignedHubName;
        private String cityName;
        private String currentCustodyStatus;
        private String listingStatus;
        private Integer turnaroundBufferMinutes;
        private VehiclePricingDto pricing;
        private List<VehicleImageDto> images;
        private List<String> features;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VehiclePricingDto {
        private BigDecimal hourlyRate;
        private BigDecimal dailyRate;
        private BigDecimal weeklyRate;
        private BigDecimal securityDepositAmount;
        private Integer freeKmPerDay;
        private BigDecimal extraKmCharge;
        private BigDecimal lateReturnHourlyPenalty;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VehicleImageDto {
        private Long id;
        private String storageObjectKey;
        private String caption;
        private boolean isPrimary;
        private Integer displayOrder;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VehicleCreateRequest {
        @NotBlank
        private String registrationNumber;
        @NotBlank
        private String vinChassisNumber;
        @NotBlank
        private String engineNumber;
        @NotBlank
        private String make;
        @NotBlank
        private String model;
        @NotNull
        private Integer yearOfManufacture;
        @NotBlank
        private String category;
        private String transmission;
        private String fuelType;
        private Integer seatingCapacity;
        private Integer odometerReadingKm;
        private Long assignedHubId;
        @NotNull
        private BigDecimal hourlyRate;
        @NotNull
        private BigDecimal dailyRate;
        private BigDecimal weeklyRate;
        @NotNull
        private BigDecimal securityDepositAmount;
        private Integer freeKmPerDay;
        private BigDecimal extraKmCharge;
        private String primaryImageUrl;
        private List<String> features;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MaintenanceBlockDto {
        private Long id;
        private Long vehicleId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String reason;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateMaintenanceBlockRequest {
        @NotNull
        private LocalDateTime startTime;
        @NotNull
        private LocalDateTime endTime;
        @NotBlank
        private String reason;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateStatusRequest {
        private String status;
        private String listingStatus;
    }
}
