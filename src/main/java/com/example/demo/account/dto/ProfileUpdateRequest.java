package com.example.demo.account.dto;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String fullName;
    private String phone;
    private String address;
    private String avatar;
    private String currentPassword;
    private String newPassword;
}
