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
        private Long targetPartnerId;
        private Long preferredHubId;
        @NotBlank
        private String make;
        @NotBlank
        private String model;
        private Integer yearOfManufacture;
        private Integer year;
        private String category;
        private String transmission;
        private String fuelType;
        private String registrationNumber;
        private String registrationPlateNumber;
        private String vinChassisNumber;
        private String engineNumber;
        private Integer currentOdometerKm;
        private String rcDocumentStorageKey;
        private String insuranceDocumentStorageKey;
        private String pickupAddressLine1;
        private String pickupAddressLine2;
        private Long pickupCityId;
        private String pickupPostalCode;

        public Integer getYearOfManufacture() {
            return yearOfManufacture != null ? yearOfManufacture : (year != null ? year : 2023);
        }

        public String getRegistrationNumber() {
            return registrationNumber != null ? registrationNumber : (registrationPlateNumber != null ? registrationPlateNumber : "AP16XX1234");
        }

        public Long getTargetPartnerId() {
            return targetPartnerId != null ? targetPartnerId : (preferredHubId != null ? preferredHubId : 1L);
        }

        public String getCategory() {
            return category != null ? category : "CAR";
        }

        public String getVinChassisNumber() {
            return vinChassisNumber != null ? vinChassisNumber : "VIN-" + System.currentTimeMillis();
        }

        public String getEngineNumber() {
            return engineNumber != null ? engineNumber : "ENG-" + System.currentTimeMillis();
        }

        public String getRcDocumentStorageKey() {
            return rcDocumentStorageKey != null ? rcDocumentStorageKey : "contributor_documents/rc_default.pdf";
        }

        public String getInsuranceDocumentStorageKey() {
            return insuranceDocumentStorageKey != null ? insuranceDocumentStorageKey : "contributor_documents/ins_default.pdf";
        }

        public String getPickupAddressLine1() {
            return pickupAddressLine1 != null ? pickupAddressLine1 : "MG Road";
        }

        public String getPickupPostalCode() {
            return pickupPostalCode != null ? pickupPostalCode : "520001";
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContributorVehicleDto {
        private Long id;
        private String make;
        private String model;
        private String plateNumber;
        private String registrationNumber;
        private String status;
        private String partnerBusinessName;
        private String partnerHubName;
        private String custodyState;
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
