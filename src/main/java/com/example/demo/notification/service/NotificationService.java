package com.example.demo.notification.service;

import com.example.demo.notification.dto.NotificationResponse;
import java.util.List;

public interface NotificationService {
    List<NotificationResponse> getMyNotifications();
    void markAsRead(Integer id);
    void markAllAsRead();
    void createNotification(Integer userId, String title, String message, String type);
    void createGlobalNotification(String title, String message, String type);
}
