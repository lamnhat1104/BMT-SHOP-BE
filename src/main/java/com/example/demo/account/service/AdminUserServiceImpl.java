package com.example.demo.account.service;

import com.example.demo.account.dto.UserResponse;
import com.example.demo.account.dto.UserSaveRequest;
import com.example.demo.account.entity.LocalAccount;
import com.example.demo.account.entity.User;
import com.example.demo.account.repository.LocalAccountRepository;
import com.example.demo.account.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final LocalAccountRepository localAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserResponse createUser(UserSaveRequest request) {
        if (userRepository.findByEmail(request.getEmail().trim()).isPresent()) {
            throw new RuntimeException("Email này đã tồn tại trên hệ thống");
        }

        User.Role roleEnum;
        try {
            roleEnum = User.Role.valueOf(request.getRole().trim().toLowerCase());
        } catch (IllegalArgumentException e) {
            roleEnum = User.Role.member;
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPhone(request.getPhone().trim());
        user.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        user.setRole(roleEnum);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        // Tạo tài khoản local tương ứng với mật khẩu mặc định "123456"
        LocalAccount localAccount = new LocalAccount();
        localAccount.setUser(savedUser);
        localAccount.setPasswordHash(passwordEncoder.encode("123456"));
        localAccount.setIsEmailVerified(true);
        localAccountRepository.save(localAccount);

        return UserResponse.fromEntity(savedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Integer userId, UserSaveRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        Optional<User> existingEmail = userRepository.findByEmail(request.getEmail().trim());
        if (existingEmail.isPresent() && !existingEmail.get().getUserId().equals(userId)) {
            throw new RuntimeException("Email này đã tồn tại trên hệ thống");
        }

        User.Role roleEnum;
        try {
            roleEnum = User.Role.valueOf(request.getRole().trim().toLowerCase());
        } catch (IllegalArgumentException e) {
            roleEnum = User.Role.member;
        }

        user.setFullName(request.getFullName().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPhone(request.getPhone().trim());
        user.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        user.setRole(roleEnum);
        user.setUpdatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        return UserResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public UserResponse toggleUserStatus(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        user.setIsActive(user.getIsActive() == null || !user.getIsActive());
        user.setUpdatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        return UserResponse.fromEntity(saved);
    }
}
