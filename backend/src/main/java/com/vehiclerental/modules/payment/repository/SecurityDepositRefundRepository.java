package com.vehiclerental.modules.payment.repository;

import com.vehiclerental.modules.payment.entity.SecurityDepositRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface SecurityDepositRefundRepository extends JpaRepository<SecurityDepositRefund, Long> {

    List<SecurityDepositRefund> findByDepositId(Long depositId);

    List<SecurityDepositRefund> findByManagerPartnerId(Long managerPartnerId);

    List<SecurityDepositRefund> findByCustomerUserId(Long customerUserId);

    List<SecurityDepositRefund> findByRefundStatus(SecurityDepositRefund.RefundStatus status);

    Optional<SecurityDepositRefund> findByManagerPartnerIdAndPartnerTransactionReference(
            Long managerPartnerId,
            String partnerTransactionReference
    );

    @Query("SELECT COALESCE(SUM(r.refundAmount), 0) FROM SecurityDepositRefund r " +
           "WHERE r.deposit.id = :depositId AND r.refundStatus <> 'CANCELLED'")
    BigDecimal getCumulativeUnvoidedRefundAmountForDeposit(@Param("depositId") Long depositId);
}
