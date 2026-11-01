package com.vehiclerental.modules.partner.repository;

import com.vehiclerental.modules.partner.entity.PartnerPaymentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartnerPaymentProfileRepository extends JpaRepository<PartnerPaymentProfile, Long> {
    Optional<PartnerPaymentProfile> findByPartnerId(Long partnerId);
}
