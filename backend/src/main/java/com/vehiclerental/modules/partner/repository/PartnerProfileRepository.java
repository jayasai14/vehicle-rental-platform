package com.vehiclerental.modules.partner.repository;

import com.vehiclerental.modules.partner.entity.PartnerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartnerProfileRepository extends JpaRepository<PartnerProfile, Long> {
    Optional<PartnerProfile> findByUserId(Long userId);
    List<PartnerProfile> findByApplicationStatus(PartnerProfile.ApplicationStatus status);
}
