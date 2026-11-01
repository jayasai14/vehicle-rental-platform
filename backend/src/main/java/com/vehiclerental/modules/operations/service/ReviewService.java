package com.vehiclerental.modules.operations.service;

import com.vehiclerental.common.exception.BadRequestException;
import com.vehiclerental.common.exception.ResourceNotFoundException;
import com.vehiclerental.modules.booking.entity.Booking;
import com.vehiclerental.modules.booking.repository.BookingRepository;
import com.vehiclerental.modules.operations.dto.OperationsDto;
import com.vehiclerental.modules.operations.entity.Review;
import com.vehiclerental.modules.operations.repository.ReviewRepository;
import com.vehiclerental.modules.user.entity.User;
import com.vehiclerental.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Transactional
    public OperationsDto.ReviewDto createReview(Long userId, OperationsDto.CreateReviewRequest request) {
        User user = userRepository.findById(userId).orElseThrow();
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getCustomerUser().getId().equals(userId)) {
            throw new BadRequestException("Only the renter can review this booking");
        }

        if (booking.getBookingStatus() != Booking.BookingStatus.COMPLETED) {
            throw new BadRequestException("Reviews are only permitted for completed bookings");
        }

        if (reviewRepository.findByBookingId(booking.getId()).isPresent()) {
            throw new BadRequestException("Review already submitted for this booking");
        }

        double avg = (request.getRatingCleanliness() + request.getRatingVehicleCondition() + request.getRatingPartnerService()) / 3.0;
        BigDecimal overall = BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);

        Review review = Review.builder()
                .booking(booking)
                .vehicle(booking.getVehicle())
                .customerUser(user)
                .ratingCleanliness(request.getRatingCleanliness())
                .ratingVehicleCondition(request.getRatingVehicleCondition())
                .ratingPartnerService(request.getRatingPartnerService())
                .overallRating(overall)
                .reviewTitle(request.getReviewTitle())
                .reviewText(request.getReviewText())
                .isPublished(true)
                .build();
        review = reviewRepository.save(review);

        return mapToDto(review);
    }

    @Transactional(readOnly = true)
    public Page<OperationsDto.ReviewDto> getVehicleReviews(Long vehicleId, Pageable pageable) {
        return reviewRepository.findByVehicleIdAndIsPublishedTrueOrderByCreatedAtDesc(vehicleId, pageable)
                .map(this::mapToDto);
    }

    @Transactional
    public OperationsDto.ReviewDto replyToReview(Long partnerUserId, Long reviewId, String replyText) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!review.getVehicle().getManagerPartner().getUser().getId().equals(partnerUserId)) {
            throw new BadRequestException("Unauthorized: Only managing partner can reply to reviews");
        }

        review.setPartnerReplyText(replyText);
        review.setPartnerRepliedAt(LocalDateTime.now());
        review = reviewRepository.save(review);

        return mapToDto(review);
    }

    private OperationsDto.ReviewDto mapToDto(Review r) {
        return OperationsDto.ReviewDto.builder()
                .id(r.getId())
                .bookingId(r.getBooking().getId())
                .vehicleId(r.getVehicle().getId())
                .customerUserId(r.getCustomerUser().getId())
                .customerName(r.getCustomerUser().getFullName())
                .ratingCleanliness(r.getRatingCleanliness())
                .ratingVehicleCondition(r.getRatingVehicleCondition())
                .ratingPartnerService(r.getRatingPartnerService())
                .overallRating(r.getOverallRating())
                .reviewTitle(r.getReviewTitle())
                .reviewText(r.getReviewText())
                .partnerReplyText(r.getPartnerReplyText())
                .partnerRepliedAt(r.getPartnerRepliedAt())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
