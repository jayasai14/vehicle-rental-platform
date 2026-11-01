package com.vehiclerental.modules.contributor.repository;

import com.vehiclerental.modules.contributor.entity.ContributionAgreementEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContributionAgreementEventRepository extends JpaRepository<ContributionAgreementEvent, Long> {
    List<ContributionAgreementEvent> findByAgreementIdOrderByEventTimestampDesc(Long agreementId);
}
