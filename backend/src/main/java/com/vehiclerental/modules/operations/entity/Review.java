package com.vehiclerental.modules.operations.entity;

import com.vehiclerental.modules.booking.entity.Booking;
import com.vehiclerental.modules.user.entity.User;
import com.vehiclerental.modules.vehicle.entity.Vehicle;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_user_id", nullable = false)
    private User customerUser;

    @Column(name = "rating_cleanliness", nullable = false)
    private Integer ratingCleanliness;

    @Column(name = "rating_vehicle_condition", nullable = false)
    private Integer ratingVehicleCondition;

    @Column(name = "rating_partner_service", nullable = false)
    private Integer ratingPartnerService;

    @Column(name = "overall_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal overallRating;

    @Column(name = "review_title", length = 150)
    private String reviewTitle;

    @Lob
    @Column(name = "review_text")
    private String reviewText;

    @Lob
    @Column(name = "partner_reply_text")
    private String partnerReplyText;

    @Column(name = "partner_replied_at")
    private LocalDateTime partnerRepliedAt;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isPublished == null) isPublished = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
