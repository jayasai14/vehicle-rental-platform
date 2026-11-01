package com.vehiclerental.modules.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentDemandDto {
        private Long id;
        private String demandReference;
        private Long holdId;
        private Long managerPartnerId;
        private String partnerBusinessName;
        private String partnerUpiId;
        private String partnerQrCodeUrl;
        private Long customerUserId;
        private String demandType;
        private BigDecimal amountDemanded;
        private BigDecimal totalVerifiedSubmitted;
        private BigDecimal amountSatisfied;
        private BigDecimal excessAmount;
        private BigDecimal cancellationFeeRetained;
        private String currency;
        private String status;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentSubmissionRequest {
        @NotNull
        private Long demandId;
        @NotBlank
        private String paymentMethod; // UPI_QR, UPI_ID, EXTERNAL_LINK, CASH_AT_PICKUP
        private String transactionReference; // UTR
        @NotNull
        private BigDecimal amountSubmitted;
        private String receiptObjectKey;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentSubmissionDto {
        private Long id;
        private String submissionReference;
        private Long demandId;
        private Long managerPartnerId;
        private Long customerUserId;
        private String customerName;
        private String paymentMethod;
        private String transactionReference;
        private BigDecimal amountSubmitted;
        private String submissionStatus;
        private LocalDateTime transactionTimestamp;
        private LocalDateTime verifiedAt;
        private String rejectionReason;
        private Long recoveredForDemandId;
        private BigDecimal reappliedAmount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VerifySubmissionRequest {
        private Boolean verified;
        private String rejectionReason;

        public boolean isVerified() {
            return verified == null || verified;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReapplyPaymentRequest {
        @NotNull
        private Long originalSubmissionId;
        @NotNull
        private Long newHoldId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RentalFeeRefundDto {
        private Long id;
        private String refundReference;
        private Long bookingId;
        private Long demandId;
        private Long managerPartnerId;
        private Long customerUserId;
        private String refundReason;
        private BigDecimal grossAmountPaid;
        private BigDecimal cancellationFeeDeducted;
        private BigDecimal refundAmount;
        private String refundStatus;
        private String refundMethod;
        private String partnerTransactionReference;
        private String receiptObjectKey;
        private LocalDateTime disbursedAt;
        private LocalDateTime acknowledgedAt;
        private String disputeReason;
        private Long disputeTicketId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecordRefundDisbursementRequest {
        @NotBlank
        private String refundMethod; // UPI_TRANSFER, BANK_NEFT_IMPS, CASH_RETURN
        @NotBlank
        private String partnerTransactionReference; // UTR
        private String receiptObjectKey;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomerRefundActionRequest {
        private boolean acknowledged;
        private String disputeReason;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SecurityDepositDto {
        private Long id;
        private Long bookingId;
        private String bookingCode;
        private Long managerPartnerId;
        private String partnerBusinessName;
        private Long customerUserId;
        private BigDecimal requiredAmount;
        private BigDecimal collectedAmount;
        private BigDecimal heldAmount;
        private BigDecimal deductedAmount;
        private BigDecimal refundedAmount;
        private String status;
        private LocalDateTime collectedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecordDepositCollectionRequest {
        @NotBlank
        private String collectionMethod;
        @NotBlank
        private String collectionTransactionReference;
        @NotNull
        private BigDecimal collectedAmount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProposeDeductionRequest {
        @NotNull
        private Long depositId;
        private Long incidentId;
        private Long inspectionId;
        @NotBlank
        private String deductionReason;
        @NotNull
        private BigDecimal proposedDeductionAmount;
        private String evidenceDocumentKey;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContributorPayableDto {
        private Long id;
        private Long bookingId;
        private String bookingCode;
        private Long agreementId;
        private Long partnerId;
        private String partnerBusinessName;
        private Long contributorUserId;
        private BigDecimal agreedCutAmount;
        private String payableStatus;
        private String partnerPaymentMethod;
        private String partnerTransactionReference;
        private LocalDateTime settledAt;
        private LocalDateTime confirmedAt;
        private String disputeReason;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecordContributorSettlementRequest {
        @NotBlank
        private String partnerPaymentMethod;
        @NotBlank
        private String partnerTransactionReference;
        private String partnerReceiptObjectKey;
    }
}
