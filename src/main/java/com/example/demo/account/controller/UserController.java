package com.example.demo.account.controller;

import com.example.demo.account.dto.ProfileResponse;
import com.example.demo.account.dto.ProfileUpdateRequest;
import com.example.demo.account.entity.LocalAccount;
import com.example.demo.account.entity.User;
import com.example.demo.account.repository.LocalAccountRepository;
import com.example.demo.account.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;
    private final LocalAccountRepository localAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Vui lòng đăng nhập!");
        }
        String email = principal.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        ProfileResponse response = ProfileResponse.builder()
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .avatar(user.getAvatar())
                .build();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(Principal principal, @RequestBody ProfileUpdateRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Vui lòng đăng nhập!");
        }
        String email = principal.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        // Validation
        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Họ tên không được để trống!");
        }

        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setAvatar(request.getAvatar());
        userRepository.save(user);

        // Xử lý đổi mật khẩu nếu được gửi lên
        if (request.getNewPassword() != null && !request.getNewPassword().trim().isEmpty()) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Vui lòng nhập mật khẩu hiện tại để đổi mật khẩu mới!");
            }

            LocalAccount localAccount = localAccountRepository.findById(user.getUserId())
                    .orElseThrow(() -> new RuntimeException("Tài khoản không hợp lệ!"));

            if (!passwordEncoder.matches(request.getCurrentPassword(), localAccount.getPasswordHash())) {
                return ResponseEntity.badRequest().body("Mật khẩu hiện tại không chính xác!");
            }

            localAccount.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            localAccountRepository.save(localAccount);
        }

        ProfileResponse response = ProfileResponse.builder()
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .avatar(user.getAvatar())
                .build();

        return ResponseEntity.ok(response);
    }
}
