package com.vehiclerental.modules.operations.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OperationsDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateTicketRequest {
        private Long bookingId;
        @NotBlank
        private String subject;
        @NotBlank
        private String category;
        private String priority;
        @NotBlank
        private String initialMessage;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TicketDto {
        private Long id;
        private String ticketReference;
        private Long userId;
        private String userName;
        private Long bookingId;
        private String subject;
        private String category;
        private String status;
        private String priority;
        private LocalDateTime createdAt;
        private List<TicketMessageDto> messages;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TicketMessageDto {
        private Long id;
        private Long senderUserId;
        private String senderName;
        private String messageText;
        private String attachmentObjectKey;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SendMessageRequest {
        @NotBlank
        private String messageText;
        private String attachmentObjectKey;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReviewDto {
        private Long id;
        private Long bookingId;
        private Long vehicleId;
        private Long customerUserId;
        private String customerName;
        private int ratingCleanliness;
        private int ratingVehicleCondition;
        private int ratingPartnerService;
        private BigDecimal overallRating;
        private String reviewTitle;
        private String reviewText;
        private String partnerReplyText;
        private LocalDateTime partnerRepliedAt;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateReviewRequest {
        private Long bookingId;
        private int ratingCleanliness;
        private int ratingVehicleCondition;
        private int ratingPartnerService;
        private String reviewTitle;
        private String reviewText;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NotificationDto {
        private Long id;
        private String title;
        private String message;
        private String notificationType;
        private boolean isRead;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdminPlatformStatsDto {
        private long totalUsers;
        private long totalPartners;
        private long totalVehicles;
        private long totalBookings;
        private long pendingPartnerApplications;
        private long openSupportTickets;
        private BigDecimal totalRentalVolume;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResolveDisputeRequest {
        private String resolutionNotes;
        private BigDecimal approvedRefundOrDeductionAmount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DisputedDepositDto {
        private Long id;
        private Long depositId;
        private Long bookingId;
        private String bookingCode;
        private String deductionReason;
        private BigDecimal proposedDeductionAmount;
        private String status;
        private String evidenceDocumentKey;
    }
}
