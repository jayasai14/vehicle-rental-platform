package com.vehiclerental.modules.location.repository;

import com.vehiclerental.modules.location.entity.PartnerHub;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartnerHubRepository extends JpaRepository<PartnerHub, Long> {
    List<PartnerHub> findByPartnerId(Long partnerId);
    List<PartnerHub> findByCityIdAndIsActiveTrue(Long cityId);
    List<PartnerHub> findByIsActiveTrue();
}
