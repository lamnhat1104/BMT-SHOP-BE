package com.example.demo.account.controller;

import com.example.demo.account.dto.*;
import com.example.demo.account.entity.SocialAccount;
import com.example.demo.account.entity.User;
import com.example.demo.account.repository.SocialAccountRepository;
import com.example.demo.account.repository.UserRepository;
import com.example.demo.account.service.AuthService;
import com.example.demo.account.service.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        try {
            String result = authService.register(request);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(@RequestBody VerifyOtpRequest request) {
        try {
            String result = authService.verifyOtp(request);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.login(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            String result = authService.forgotPassword(request);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            String result = authService.resetPassword(request);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/oauth2-success")
    public void oauth2Success(@AuthenticationPrincipal OAuth2User principal, HttpServletResponse response) throws IOException {
        if (principal == null) {
            response.sendRedirect("http://localhost:5173/login?error=unauthorized");
            return;
        }

        String provider = principal.getAttribute("provider");
        String providerId = principal.getAttribute("providerId");

        User user = null;
        if (provider != null && providerId != null) {
            try {
                SocialAccount.Provider providerEnum = SocialAccount.Provider.valueOf(provider);
                user = socialAccountRepository.findByProviderAndProviderId(providerEnum, providerId)
                        .map(SocialAccount::getUser)
                        .orElse(null);
            } catch (IllegalArgumentException e) {
                // Ignore invalid enum
            }
        }

        if (user == null) {
            String email = principal.getAttribute("email");
            user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Lỗi hệ thống sau khi login Social"));
        }

        String token = jwtService.generateToken(user.getEmail());

        String redirectUrl = String.format("http://localhost:5173/?token=%s&fullName=%s&role=%s",
                token,
                URLEncoder.encode(user.getFullName(), StandardCharsets.UTF_8),
                user.getRole().name());

        response.sendRedirect(redirectUrl);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        return ResponseEntity.ok("Đăng xuất thành công!");
    }
}
