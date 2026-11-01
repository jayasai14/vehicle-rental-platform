package com.vehiclerental.modules.payment.repository;

import com.vehiclerental.modules.payment.entity.SecurityDeposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SecurityDepositRepository extends JpaRepository<SecurityDeposit, Long> {
    Optional<SecurityDeposit> findByBookingId(Long bookingId);
    List<SecurityDeposit> findByManagerPartnerId(Long managerPartnerId);
    List<SecurityDeposit> findByCustomerUserId(Long customerUserId);
}
