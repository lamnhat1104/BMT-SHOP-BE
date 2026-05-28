package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Mã xác thực đăng ký tài khoản");
        message.setText("Mã OTP của bạn là: " + otp + ". Mã có hiệu lực trong 5 phút.");
        mailSender.send(message);
    }

    public void sendResetPasswordEmail(String toEmail, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Yêu cầu đặt lại mật khẩu");
        String resetLink = "http://localhost:5173/reset-password?token=" + token;
        message.setText("Bạn đã yêu cầu đặt lại mật khẩu. Vui lòng nhấn vào link bên dưới để thực hiện:\n" + resetLink + "\n\nLink có hiệu lực trong 15 phút.");
        mailSender.send(message);
    }

    public void sendOrderConfirmationEmail(String toEmail, String orderCode, Double totalAmount, String fullName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Đặt hàng thành công - Đơn hàng #" + orderCode);
            message.setText("Chào " + fullName + ",\n\n" +
                    "Đơn hàng của bạn đã được đặt thành công trên BMT SHOP.\n" +
                    "Mã đơn hàng của bạn: " + orderCode + "\n" +
                    "Tổng tiền thanh toán: " + String.format("%,.0f VNĐ", totalAmount) + "\n\n" +
                    "Chúng tôi sẽ nhanh chóng chuẩn bị và liên hệ giao hàng cho bạn trong thời gian sớm nhất.\n" +
                    "Bạn có thể sử dụng mã đơn hàng và số điện thoại của mình để tra cứu tiến độ đơn hàng tại mục Tra cứu đơn hàng trên trang web.\n\n" +
                    "Cảm ơn bạn đã tin tưởng mua sắm tại BMT SHOP!\n\n" +
                    "Trân trọng,\n" +
                    "Đội ngũ BMT SHOP");
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("EmailService - Lỗi gửi mail xác nhận đơn hàng: " + e.getMessage());
        }
    }
}
