package com.vehiclerental.modules.booking.repository;

import com.vehiclerental.modules.booking.entity.InspectionMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InspectionMediaRepository extends JpaRepository<InspectionMedia, Long> {
    List<InspectionMedia> findByInspectionId(Long inspectionId);
}
