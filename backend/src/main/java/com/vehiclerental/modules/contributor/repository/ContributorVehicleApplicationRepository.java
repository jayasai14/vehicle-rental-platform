package com.vehiclerental.modules.contributor.repository;

import com.vehiclerental.modules.contributor.entity.ContributorVehicleApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContributorVehicleApplicationRepository extends JpaRepository<ContributorVehicleApplication, Long> {
    List<ContributorVehicleApplication> findByContributorId(Long contributorId);
    List<ContributorVehicleApplication> findByTargetPartnerId(Long partnerId);
    List<ContributorVehicleApplication> findByStatus(ContributorVehicleApplication.ApplicationStatus status);
    Optional<ContributorVehicleApplication> findByRegistrationNumber(String registrationNumber);
}
