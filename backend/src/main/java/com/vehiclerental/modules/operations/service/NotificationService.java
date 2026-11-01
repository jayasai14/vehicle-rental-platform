package com.vehiclerental.modules.operations.service;

import com.vehiclerental.common.exception.ResourceNotFoundException;
import com.vehiclerental.modules.operations.dto.OperationsDto;
import com.vehiclerental.modules.operations.entity.Notification;
import com.vehiclerental.modules.operations.repository.NotificationRepository;
import com.vehiclerental.modules.user.entity.User;
import com.vehiclerental.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void sendNotification(Long userId, String title, String message, String type, String refType, String refId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        Notification n = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .notificationType(type)
                .referenceEntityType(refType)
                .referenceEntityId(refId)
                .isRead(false)
                .build();
        notificationRepository.save(n);
    }

    @Transactional(readOnly = true)
    public Page<OperationsDto.NotificationDto> getUserNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(n -> OperationsDto.NotificationDto.builder()
                        .id(n.getId())
                        .title(n.getTitle())
                        .message(n.getMessage())
                        .notificationType(n.getNotificationType())
                        .isRead(n.getIsRead())
                        .createdAt(n.getCreatedAt())
                        .build());
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (n.getUser().getId().equals(userId)) {
            n.setIsRead(true);
            n.setReadAt(LocalDateTime.now());
            notificationRepository.save(n);
        }
    }
}
