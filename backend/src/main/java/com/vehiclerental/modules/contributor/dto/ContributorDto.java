package com.vehiclerental.modules.contributor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ContributorDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContributorProfileRequest {
        @NotBlank
        private String panNumber;
        @NotBlank
        private String bankAccountNumber;
        @NotBlank
        private String bankIfscCode;
        @NotBlank
        private String bankName;
        @NotBlank
        private String addressLine1;
        private String addressLine2;
        private Long cityId;
        @NotBlank
        private String postalCode;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContributorProfileDto {
        private Long id;
        private Long userId;
        private String panNumber;
        private String bankAccountNumber;
        private String bankIfscCode;
        private String bankName;
        private boolean active;
        private String address;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VehicleApplicationRequest {
        @NotNull
        private Long targetPartnerId;
        @NotBlank
        private String make;
        @NotBlank
        private String model;
        @NotNull
        private Integer yearOfManufacture;
        @NotBlank
        private String category; // CAR, BIKE, SCOOTER
        private String transmission;
        private String fuelType;
        @NotBlank
        private String registrationNumber;
        @NotBlank
        private String vinChassisNumber;
        @NotBlank
        private String engineNumber;
        private Integer currentOdometerKm;
        @NotBlank
        private String rcDocumentStorageKey;
        @NotBlank
        private String insuranceDocumentStorageKey;
        @NotBlank
        private String pickupAddressLine1;
        private String pickupAddressLine2;
        private Long pickupCityId;
        @NotBlank
        private String pickupPostalCode;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContributorApplicationDto {
        private Long id;
        private Long contributorId;
        private Long targetPartnerId;
        private String targetPartnerBusinessName;
        private String make;
        private String model;
        private Integer yearOfManufacture;
        private String category;
        private String transmission;
        private String fuelType;
        private String registrationNumber;
        private String status;
        private String rejectionReason;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProposeAgreementRequest {
        @NotNull
        private Long applicationId;
        @NotNull
        private BigDecimal fixedContributorCutPerBooking;
        @NotNull
        private LocalDate effectiveStartDate;
        @NotNull
        private LocalDate effectiveEndDate;
        private Integer noticePeriodDays;
        @NotBlank
        private String termsAndConditionsText;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AgreementDto {
        private Long id;
        private Long applicationId;
        private Long partnerId;
        private String partnerBusinessName;
        private Long contributorId;
        private Long vehicleId;
        private String agreementCode;
        private BigDecimal fixedContributorCutPerBooking;
        private LocalDate effectiveStartDate;
        private LocalDate effectiveEndDate;
        private Integer noticePeriodDays;
        private String termsAndConditionsText;
        private String status;
        private LocalDateTime partnerSignedAt;
        private LocalDateTime contributorSignedAt;
        private LocalDateTime createdAt;
    }
}
