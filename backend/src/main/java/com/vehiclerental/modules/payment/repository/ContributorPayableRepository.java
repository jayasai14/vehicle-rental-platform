package com.vehiclerental.modules.payment.repository;

import com.vehiclerental.modules.payment.entity.ContributorPayable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContributorPayableRepository extends JpaRepository<ContributorPayable, Long> {
    Optional<ContributorPayable> findByBookingId(Long bookingId);
    List<ContributorPayable> findByPartnerId(Long partnerId);
    List<ContributorPayable> findByContributorUserId(Long contributorUserId);
    List<ContributorPayable> findByPayableStatus(ContributorPayable.PayableStatus status);
}
