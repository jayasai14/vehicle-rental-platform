package com.vehiclerental.modules.location.repository;

import com.vehiclerental.modules.location.entity.PickupLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PickupLocationRepository extends JpaRepository<PickupLocation, Long> {
    List<PickupLocation> findByPartnerId(Long partnerId);
    List<PickupLocation> findByPartnerHubId(Long hubId);
    List<PickupLocation> findByIsActiveTrue();
}
