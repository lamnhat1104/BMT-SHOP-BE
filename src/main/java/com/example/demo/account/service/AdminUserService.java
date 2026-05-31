package com.example.demo.account.service;

import com.example.demo.account.dto.UserResponse;
import com.example.demo.account.dto.UserSaveRequest;
import java.util.List;

public interface AdminUserService {
    List<UserResponse> getAllUsers();
    UserResponse createUser(UserSaveRequest request);
    UserResponse updateUser(Integer userId, UserSaveRequest request);
    UserResponse toggleUserStatus(Integer userId);
}
