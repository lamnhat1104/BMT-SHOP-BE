package com.example.demo.notification.controller;

import com.example.demo.notification.dto.NotificationResponse;
import com.example.demo.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications() {
        return ResponseEntity.ok(notificationService.getMyNotifications());
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Integer id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/test")
    public ResponseEntity<Void> createTestNotification() {
        notificationService.createGlobalNotification("Thông báo thử nghiệm", "Đây là thông báo test từ hệ thống!", "SYSTEM");
        return ResponseEntity.ok().build();
    }
}
