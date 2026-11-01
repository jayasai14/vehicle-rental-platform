package com.vehiclerental.modules.contributor.repository;

import com.vehiclerental.modules.contributor.entity.VehicleWithdrawalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleWithdrawalRequestRepository extends JpaRepository<VehicleWithdrawalRequest, Long> {
    List<VehicleWithdrawalRequest> findByAgreementId(Long agreementId);
}
