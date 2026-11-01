package com.vehiclerental.modules.operations.repository;

import com.vehiclerental.modules.operations.entity.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxEvent.OutboxStatus status, Pageable pageable);
}
