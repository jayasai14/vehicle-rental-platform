package com.vehiclerental.modules.partner.repository;

import com.vehiclerental.modules.partner.entity.PartnerDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartnerDocumentRepository extends JpaRepository<PartnerDocument, Long> {
    List<PartnerDocument> findByPartnerId(Long partnerId);
}
