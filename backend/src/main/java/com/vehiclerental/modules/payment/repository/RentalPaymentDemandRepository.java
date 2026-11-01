package com.vehiclerental.modules.payment.repository;

import com.vehiclerental.modules.payment.entity.RentalPaymentDemand;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RentalPaymentDemandRepository extends JpaRepository<RentalPaymentDemand, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM RentalPaymentDemand d WHERE d.id = :id")
    Optional<RentalPaymentDemand> findByIdWithLock(@Param("id") Long id);

    Optional<RentalPaymentDemand> findByDemandReference(String demandReference);

    Optional<RentalPaymentDemand> findByHoldId(Long holdId);

    List<RentalPaymentDemand> findByCustomerUserId(Long customerUserId);

    List<RentalPaymentDemand> findByManagerPartnerId(Long managerPartnerId);
}
