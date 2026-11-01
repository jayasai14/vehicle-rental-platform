package com.vehiclerental.modules.user.repository;

import com.vehiclerental.modules.user.entity.DrivingLicence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DrivingLicenceRepository extends JpaRepository<DrivingLicence, Long> {
    Optional<DrivingLicence> findByUserId(Long userId);
    Optional<DrivingLicence> findByLicenceNumber(String licenceNumber);
}
