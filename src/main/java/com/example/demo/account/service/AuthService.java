package com.example.demo.account.service;

import com.example.demo.account.dto.*;
import com.example.demo.account.entity.LocalAccount;
import com.example.demo.account.entity.PasswordReset;
import com.example.demo.account.entity.User;
import com.example.demo.account.repository.LocalAccountRepository;
import com.example.demo.account.repository.PasswordResetRepository;
import com.example.demo.account.repository.UserRepository;
import com.example.demo.service.EmailService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final LocalAccountRepository localAccountRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtService jwtService;

    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống!"));

        String token = UUID.randomUUID().toString();

        passwordResetRepository.deleteByEmail(request.getEmail());

        PasswordReset passwordReset = new PasswordReset();
        passwordReset.setEmail(request.getEmail());
        passwordReset.setToken(token);
        passwordReset.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        passwordResetRepository.save(passwordReset);

        emailService.sendResetPasswordEmail(request.getEmail(), token);

        return "Link đặt lại mật khẩu đã được gửi tới email của bạn!";
    }

    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        PasswordReset passwordReset = passwordResetRepository.findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Link không hợp lệ hoặc đã hết hạn!"));

        if (passwordReset.getExpiresAt().isBefore(LocalDateTime.now())) {
            passwordResetRepository.delete(passwordReset);
            throw new RuntimeException("Link đã hết hạn!");
        }

        User user = userRepository.findByEmail(passwordReset.getEmail())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        LocalAccount localAccount = localAccountRepository.findById(user.getUserId())
                .orElse(new LocalAccount());
        
        if (localAccount.getUser() == null) {
            localAccount.setUser(user);
            localAccount.setIsEmailVerified(true);
        }

        localAccount.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        localAccountRepository.save(localAccount);

        passwordResetRepository.delete(passwordReset);

        return "Đặt lại mật khẩu thành công!";
    }

    @Transactional
    public String register(RegisterRequest request) {
        Optional<User> existingUser = userRepository.findByEmail(request.getEmail());

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();

            if (user.getIsActive()) {
                throw new RuntimeException("Email đã được sử dụng và xác thực!");
            }

            user.setFullName(request.getFullName());
            user.setPhone(request.getPhone());
        } else {
            user = new User();
            user.setFullName(request.getFullName());
            user.setEmail(request.getEmail());
            user.setPhone(request.getPhone());
            user.setIsActive(false);
        }

        User savedUser = userRepository.save(user);

        String otp = String.format("%06d", new Random().nextInt(999999));

        LocalAccount localAccount = localAccountRepository.findById(savedUser.getUserId())
                .orElse(new LocalAccount());

        localAccount.setUser(savedUser);
        localAccount.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        localAccount.setVerificationCode(otp);
        localAccount.setCodeExpiredAt(LocalDateTime.now().plusMinutes(5));
        localAccount.setIsEmailVerified(false);

        localAccountRepository.save(localAccount);

        emailService.sendOtpEmail(request.getEmail(), otp);

        return "Mã xác thực mới đã được gửi đến email của bạn!";
    }

    @Transactional
    public String verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        LocalAccount localAccount = localAccountRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Tài khoản không hợp lệ!"));

        if (localAccount.getVerificationCode() == null ||
                !localAccount.getVerificationCode().equals(request.getOtp())) {
            throw new RuntimeException("Mã OTP không chính xác!");
        }

        if (localAccount.getCodeExpiredAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Mã OTP đã hết hạn!");
        }

        user.setIsActive(true);
        userRepository.save(user);

        localAccount.setVerificationCode(null);
        localAccount.setCodeExpiredAt(null);
        localAccount.setIsEmailVerified(true);
        localAccountRepository.save(localAccount);

        return "Xác thực tài khoản thành công! Bây giờ bạn có thể đăng nhập.";
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email hoặc mật khẩu không đúng!"));

        if (!user.getIsActive()) {
            throw new RuntimeException("Tài khoản chưa được kích hoạt OTP!");
        }

        LocalAccount localAccount = localAccountRepository.findById(user.getUserId()).orElseThrow(() -> new RuntimeException("Email hoặc mật khẩu không đúng!"));
        if (!passwordEncoder.matches(request.getPassword(), localAccount.getPasswordHash())) {
            throw new RuntimeException("Email hoặc mật khẩu không đúng!");
        }

        String token = jwtService.generateToken(user.getEmail());
        return new LoginResponse(user.getUserId(), token, user.getFullName(), user.getRole().name());
    }
}
