package com.example.demo.account.controller;

import com.example.demo.account.dto.UserRegistrationDto;
import com.example.demo.account.service.UserService;
import com.example.demo.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> registerUser(@Valid @RequestBody UserRegistrationDto dto) {
        try {
            userService.registerUser(dto);
            return ResponseEntity.ok(new ApiResponse(true, "Đăng ký thành công"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }
}
