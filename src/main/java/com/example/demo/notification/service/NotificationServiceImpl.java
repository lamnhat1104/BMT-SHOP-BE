package com.example.demo.notification.service;

import com.example.demo.account.entity.User;
import com.example.demo.account.repository.UserRepository;
import com.example.demo.notification.dto.NotificationResponse;
import com.example.demo.notification.entity.Notification;
import com.example.demo.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Vui lòng đăng nhập để thực hiện thao tác này!"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications() {
        User user = getCurrentUser();
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId())
                .stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markAsRead(Integer id) {
        User user = getCurrentUser();
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Thông báo không tồn tại"));

        if (!notification.getUserId().equals(user.getUserId())) {
            throw new RuntimeException("Bạn không có quyền sửa thông báo này");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        User user = getCurrentUser();
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(user.getUserId(), false);
        for (Notification n : unread) {
            n.setIsRead(true);
        }
        notificationRepository.saveAll(unread);
    }

    @Override
    @Transactional
    public void createNotification(Integer userId, String title, String message, String type) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(type)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void createGlobalNotification(String title, String message, String type) {
        // Send to all users except admins (for simplicity, we just send to all regular users)
        // Note: For a very large user base, this should be done asynchronously or using a broadcast mechanism.
        List<User> users = userRepository.findAll();
        List<Notification> notifications = users.stream()
                .map(u -> Notification.builder()
                        .userId(u.getUserId())
                        .title(title)
                        .message(message)
                        .type(type)
                        .isRead(false)
                        .build())
                .collect(Collectors.toList());
        notificationRepository.saveAll(notifications);
    }
}
