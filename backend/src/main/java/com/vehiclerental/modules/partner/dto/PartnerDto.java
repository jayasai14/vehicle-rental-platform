package com.vehiclerental.modules.partner.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class PartnerDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PartnerApplicationRequest {
        @NotBlank
        private String businessName;
        @NotBlank
        private String panNumber;
        private String gstin;
        private String tradeLicenceNumber;
        private String commercialRegistrationNumber;
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
    public static class PartnerProfileDto {
        private Long id;
        private Long userId;
        private String businessName;
        private String panNumber;
        private String gstin;
        private String tradeLicenceNumber;
        private String commercialRegistrationNumber;
        private String verificationStatus;
        private LocalDateTime verifiedAt;
        private String rejectionReason;
        private boolean active;
        private String registeredAddress;
        private List<PartnerDocumentDto> documents;
        private PartnerPaymentProfileDto paymentProfile;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PartnerDocumentDto {
        private Long id;
        private String documentType;
        private String documentNumber;
        private String storageObjectKey;
        private String verificationStatus;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UploadDocumentRequest {
        @NotBlank
        private String documentType;
        @NotBlank
        private String documentNumber;
        @NotBlank
        private String storageObjectKey;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PartnerPaymentProfileDto {
        private Long id;
        private Long partnerId;
        private String payeeDisplayName;
        private String upiId;
        private String upiQrObjectKey;
        private String externalPaymentUrl;
        private String acceptedPaymentMethods;
        private boolean cashAtPickupEnabled;
        private boolean active;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConfigurePaymentProfileRequest {
        @NotBlank
        private String payeeDisplayName;
        @NotBlank
        private String upiId;
        private String upiQrObjectKey;
        private String externalPaymentUrl;
        private String acceptedPaymentMethods; // e.g. '["UPI_QR", "UPI_ID", "CASH_AT_PICKUP"]'
        private boolean cashAtPickupEnabled;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewApplicationRequest {
        private boolean approved;
        private String rejectionReason;
    }
}
