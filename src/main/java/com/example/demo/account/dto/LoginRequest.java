package com.example.demo.account.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}
