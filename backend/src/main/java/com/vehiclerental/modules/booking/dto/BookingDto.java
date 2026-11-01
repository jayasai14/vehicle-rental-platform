package com.vehiclerental.modules.booking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class BookingDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateHoldRequest {
        @NotNull
        private Long vehicleId;
        @NotNull
        private Long pickupLocationId;
        private Long dropoffLocationId;
        private Long returnLocationId;
        @NotNull
        private LocalDateTime startTime;
        @NotNull
        private LocalDateTime endTime;

        public Long getDropoffLocationId() {
            return dropoffLocationId != null ? dropoffLocationId : returnLocationId;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HoldResponseDto {
        private Long id;
        private String holdReference;
        private Long vehicleId;
        private String vehicleName;
        private String vehicleRegistration;
        private Long customerUserId;
        private Long pickupLocationId;
        private String pickupLocationName;
        private Long dropoffLocationId;
        private String dropoffLocationName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private BigDecimal quotedRentalAmount;
        private BigDecimal quotedDepositAmount;
        private BigDecimal quotedSurchargeAmount;
        private BigDecimal totalHoldAmount;
        private String status;
        private LocalDateTime expiresAt;
        private Long initialDemandId;
        private String initialDemandReference;
        private Long partnerId;
        private String partnerBusinessName;
        private String partnerUpiId;
        private String partnerQrKey;
        private boolean cashAtPickupEnabled;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookingResponseDto {
        private Long id;
        private String bookingCode;
        private Long holdId;
        private Long vehicleId;
        private String vehicleName;
        private String vehicleRegistration;
        private Long customerUserId;
        private String customerName;
        private String customerPhone;
        private Long managerPartnerId;
        private String partnerBusinessName;
        private Long pickupLocationId;
        private String pickupLocationName;
        private Long dropoffLocationId;
        private String dropoffLocationName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private LocalDateTime actualReturnTime;
        private BigDecimal baseRentalAmount;
        private BigDecimal securityDepositAmount;
        private BigDecimal deliverySurchargeAmount;
        private BigDecimal totalAmountPayable;
        private String bookingStatus;
        private String status;
        private String custodyState;
        private String cancellationReason;
        private LocalDateTime cancelledAt;
        private LocalDateTime createdAt;
        private Long initialDemandId;
        private String initialDemandReference;
        private String demandStatus;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExtensionRequestDto {
        @NotNull
        private LocalDateTime requestedEndTime;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExtensionResponseDto {
        private Long id;
        private Long bookingId;
        private LocalDateTime previousEndTime;
        private LocalDateTime requestedEndTime;
        private BigDecimal extensionRentalAmount;
        private Long demandId;
        private String status;
        private String decisionReason;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubmitInspectionRequest {
        @NotNull
        private Long bookingId;
        @NotNull
        private String inspectionType; // PRE_RENTAL_HANDOVER, POST_RENTAL_RETURN
        @NotNull
        private Integer odometerReadingKm;
        @NotNull
        private Integer fuelOrBatteryLevelPercent;
        private String exteriorConditionNotes;
        private String interiorConditionNotes;
        private String knownScratchesOrDents;
        private boolean customerSignatureConfirmed;
        private boolean partnerSignatureConfirmed;
        private List<InspectionMediaItem> media;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InspectionMediaItem {
        private String viewAngle;
        private String storageObjectKey;
        private String caption;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InspectionResponseDto {
        private Long id;
        private Long bookingId;
        private Long vehicleId;
        private String inspectionType;
        private Integer odometerReadingKm;
        private Integer fuelOrBatteryLevelPercent;
        private String exteriorConditionNotes;
        private String interiorConditionNotes;
        private String knownScratchesOrDents;
        private boolean customerSignatureConfirmed;
        private boolean partnerSignatureConfirmed;
        private LocalDateTime conductedAt;
        private List<InspectionMediaItem> media;
    }
}
