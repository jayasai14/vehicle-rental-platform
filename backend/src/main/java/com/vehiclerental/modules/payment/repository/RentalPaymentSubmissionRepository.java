package com.vehiclerental.modules.payment.repository;

import com.vehiclerental.modules.payment.entity.RentalPaymentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RentalPaymentSubmissionRepository extends JpaRepository<RentalPaymentSubmission, Long> {

    Optional<RentalPaymentSubmission> findBySubmissionReference(String submissionReference);

    List<RentalPaymentSubmission> findByDemandId(Long demandId);

    List<RentalPaymentSubmission> findByManagerPartnerId(Long managerPartnerId);

    Optional<RentalPaymentSubmission> findByManagerPartnerIdAndTransactionReference(
            Long managerPartnerId,
            String transactionReference
    );

    List<RentalPaymentSubmission> findByCustomerUserId(Long customerUserId);
}
