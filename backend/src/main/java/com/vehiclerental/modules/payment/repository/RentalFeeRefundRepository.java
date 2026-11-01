package com.vehiclerental.modules.payment.repository;

import com.vehiclerental.modules.payment.entity.RentalFeeRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface RentalFeeRefundRepository extends JpaRepository<RentalFeeRefund, Long> {

    Optional<RentalFeeRefund> findByRefundReference(String refundReference);

    List<RentalFeeRefund> findByBookingId(Long bookingId);

    List<RentalFeeRefund> findByDemandId(Long demandId);

    List<RentalFeeRefund> findByManagerPartnerId(Long managerPartnerId);

    List<RentalFeeRefund> findByCustomerUserId(Long customerUserId);

    @Query("SELECT COALESCE(SUM(r.refundAmount), 0) FROM RentalFeeRefund r " +
           "WHERE r.demand.id = :demandId AND r.refundStatus <> 'CANCELLED'")
    BigDecimal getCumulativeUnvoidedRefundAmountForDemand(@Param("demandId") Long demandId);

    Optional<RentalFeeRefund> findByManagerPartnerIdAndPartnerTransactionReference(
            Long managerPartnerId,
            String partnerTransactionReference
    );
}
