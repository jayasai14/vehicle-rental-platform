package com.vehiclerental.modules.contributor.repository;

import com.vehiclerental.modules.contributor.entity.ContributionAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContributionAgreementRepository extends JpaRepository<ContributionAgreement, Long> {
    Optional<ContributionAgreement> findByAgreementCode(String agreementCode);
    Optional<ContributionAgreement> findByApplicationId(Long applicationId);
    List<ContributionAgreement> findByContributorId(Long contributorId);
    List<ContributionAgreement> findByPartnerId(Long partnerId);
    Optional<ContributionAgreement> findByVehicleId(Long vehicleId);
}
