package com.vehiclerental.modules.contributor.entity;

import com.vehiclerental.modules.partner.entity.PartnerProfile;
import com.vehiclerental.modules.vehicle.entity.Vehicle;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "contribution_agreements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContributionAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private ContributorVehicleApplication application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private PartnerProfile partner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contributor_id", nullable = false)
    private ContributorProfile contributor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Column(name = "agreement_code", nullable = false, unique = true, length = 32)
    private String agreementCode;

    @Column(name = "fixed_contributor_cut_per_booking", nullable = false, precision = 10, scale = 2)
    private BigDecimal fixedContributorCutPerBooking;

    @Column(name = "effective_start_date", nullable = false)
    private LocalDate effectiveStartDate;

    @Column(name = "effective_end_date", nullable = false)
    private LocalDate effectiveEndDate;

    @Column(name = "notice_period_days", nullable = false)
    private Integer noticePeriodDays;

    @Lob
    @Column(name = "terms_and_conditions_text", nullable = false)
    private String termsAndConditionsText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AgreementStatus status;

    @Column(name = "partner_signed_at")
    private LocalDateTime partnerSignedAt;

    @Column(name = "contributor_signed_at")
    private LocalDateTime contributorSignedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (noticePeriodDays == null) noticePeriodDays = 7;
        if (status == null) status = AgreementStatus.PENDING_CONTRIBUTOR_SIGNATURE;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum AgreementStatus {
        PENDING_PARTNER_SIGNATURE,
        PENDING_CONTRIBUTOR_SIGNATURE,
        ACTIVE,
        TERMINATION_REQUESTED,
        TERMINATED,
        EXPIRED
    }
}
