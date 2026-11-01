package com.vehiclerental.modules.payment.repository;

import com.vehiclerental.modules.payment.entity.DepositDeduction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepositDeductionRepository extends JpaRepository<DepositDeduction, Long> {
    List<DepositDeduction> findByDepositId(Long depositId);
    List<DepositDeduction> findByStatus(DepositDeduction.DeductionStatus status);
}
