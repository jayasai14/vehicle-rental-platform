package com.vehiclerental.modules.operations.service;

import com.vehiclerental.modules.operations.entity.AuditLog;
import com.vehiclerental.modules.operations.repository.AuditLogRepository;
import com.vehiclerental.modules.user.entity.User;
import com.vehiclerental.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public void recordAction(Long actorUserId, String action, String entityName, String entityId, String details) {
        User actor = actorUserId != null ? userRepository.findById(actorUserId).orElse(null) : null;
        AuditLog log = AuditLog.builder()
                .actorUser(actor)
                .action(action)
                .entityName(entityName)
                .entityId(entityId)
                .details(details)
                .build();
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getLogs(Pageable pageable) {
        return auditLogRepository.findByOrderByCreatedAtDesc(pageable);
    }
}
