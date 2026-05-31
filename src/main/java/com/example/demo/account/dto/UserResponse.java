package com.example.demo.account.dto;

import com.example.demo.account.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Integer userId;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String avatar;
    private String role;
    private Integer isActive; // 1 = Active, 0 = Blocked
    private LocalDateTime createdAt;

    public static UserResponse fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .avatar(user.getAvatar())
                .role(user.getRole() != null ? user.getRole().name() : "member")
                .isActive(user.getIsActive() != null && user.getIsActive() ? 1 : 0)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
