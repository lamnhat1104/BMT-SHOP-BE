package com.example.demo.account.controller;

import com.example.demo.account.dto.UserResponse;
import com.example.demo.account.dto.UserSaveRequest;
import com.example.demo.account.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(adminUserService.getAllUsers());
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserSaveRequest request) {
        return ResponseEntity.ok(adminUserService.createUser(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Integer id,
            @Valid @RequestBody UserSaveRequest request) {
        return ResponseEntity.ok(adminUserService.updateUser(id, request));
    }

    @PostMapping("/{id}/toggle-status")
    public ResponseEntity<UserResponse> toggleUserStatus(@PathVariable Integer id) {
        return ResponseEntity.ok(adminUserService.toggleUserStatus(id));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<UserResponse> resetPassword(
            @PathVariable Integer id,
            @RequestBody java.util.Map<String, String> requestBody) {
        String newPassword = requestBody.get("newPassword");
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new RuntimeException("Mật khẩu mới không được để trống");
        }
        return ResponseEntity.ok(adminUserService.resetPassword(id, newPassword));
    }
}
