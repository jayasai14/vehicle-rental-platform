package com.vehiclerental.modules.contributor.repository;

import com.vehiclerental.modules.contributor.entity.ContributorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContributorProfileRepository extends JpaRepository<ContributorProfile, Long> {
    Optional<ContributorProfile> findByUserId(Long userId);
    Optional<ContributorProfile> findByPanNumber(String panNumber);
}
