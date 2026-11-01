package com.vehiclerental.modules.user.repository;

import com.vehiclerental.modules.user.entity.KycVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KycVerificationRepository extends JpaRepository<KycVerification, Long> {
    List<KycVerification> findByUserId(Long userId);
    Optional<KycVerification> findByUserIdAndKycType(Long userId, KycVerification.KycType kycType);
}
