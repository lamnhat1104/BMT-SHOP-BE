package com.example.demo.payment;

import com.example.demo.order.entity.Order;
import com.example.demo.order.entity.OrderDetail;
import com.example.demo.order.repository.OrderRepository;
import com.example.demo.product.entity.Product;
import com.example.demo.product.repository.ProductRepository;
import com.example.demo.account.repository.UserRepository;
import com.example.demo.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final VNPAYConfig vnpayConfig;

    @GetMapping("/vnpay-callback")
    @Transactional
    public void vnpayCallback(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.info("Nhận callback từ VNPay");
        
        // Lấy tất cả tham số từ VNPay gửi về bằng TreeMap để tự động sắp xếp
        Map<String, String> fields = new TreeMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");

        // Tính toán signature để đối chiếu
        StringBuilder hashData = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String fieldName = entry.getKey();
            String fieldValue = entry.getValue();

            if (fieldValue != null && !fieldValue.isEmpty()) {
                if (!first) {
                    hashData.append('&');
                }
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(fieldValue);
                first = false;
            }
        }

        String signValue =
                vnpayConfig.hmacSHA512(
                        vnpayConfig.getHashSecret(),
                        hashData.toString()
                );

        log.info("Callback HashData: {}", hashData);
        log.info("Generated Sign: {}", signValue);
        log.info("VNPay Sign: {}", vnp_SecureHash);
        
        String orderCode = request.getParameter("vnp_TxnRef");
        String responseCode = request.getParameter("vnp_ResponseCode");
        
        // Mặc định chuyển hướng về frontend (cổng mặc định là 5173 cho Vite)
        String frontendRedirectUrl = "http://localhost:5173/checkout";
        
        if (signValue.equalsIgnoreCase(vnp_SecureHash)) {
            // Chữ ký hợp lệ
            Optional<Order> orderOpt = orderRepository.findByOrderCode(orderCode);
            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();
                if ("00".equals(responseCode)) {
                    // Thanh toán thành công
                    order.setPaymentStatus("paid");
                    order.setStatus("Chờ xác nhận");
                    orderRepository.save(order);
                    
                    // Gửi email xác nhận đơn hàng sau khi thanh toán thành công
                    try {
                        userRepository.findById(order.getUserId()).ifPresent(user -> {
                            emailService.sendOrderConfirmationEmail(user.getEmail(), orderCode, order.getTotalPrice(), order.getReceiverName());
                        });
                    } catch (Exception e) {
                        log.error("Lỗi khi gửi email sau khi thanh toán thành công: {}", e.getMessage());
                    }
                    
                    response.sendRedirect(frontendRedirectUrl + "?vnpay=success&orderCode=" + orderCode + "&phone=" + URLEncoder.encode(order.getReceiverPhone(), StandardCharsets.UTF_8.toString()));
                    return;
                } else {
                    // Thanh toán thất bại hoặc người dùng hủy
                    order.setPaymentStatus("failed");
                    order.setStatus("Đã hủy");
                    
                    // Hoàn lại kho hàng
                    if (order.getOrderDetails() != null) {
                        for (OrderDetail detail : order.getOrderDetails()) {
                            Product product = detail.getProduct();
                            if (product != null) {
                                product.setStock(product.getStock() + detail.getQuantity());
                                product.setStatus("available");
                                productRepository.save(product);
                            }
                        }
                    }
                    orderRepository.save(order);
                    response.sendRedirect(frontendRedirectUrl + "?vnpay=failed&orderCode=" + orderCode);
                    return;
                }
            } else {
                log.error("Không tìm thấy đơn hàng: {}", orderCode);
                response.sendRedirect(frontendRedirectUrl + "?vnpay=failed&error=order_not_found");
                return;
            }
        } else {
            log.error("Sai chữ ký bảo mật từ VNPay callback. hashData calculated: '{}', signValue calculated: '{}', vnp_SecureHash received: '{}'", hashData.toString(), signValue, vnp_SecureHash);
            response.sendRedirect(frontendRedirectUrl + "?vnpay=failed&error=invalid_signature");
            return;
        }
    }
}
