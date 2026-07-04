package com.example.demo.ai.config;

import com.example.demo.order.entity.Order;
import com.example.demo.order.repository.OrderRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

@Configuration
public class AiConfig {

    public record OrderStatusRequest(String orderCode) {}
    public record OrderStatusResponse(String status, String message) {}

    @Bean
    @Description("Lấy trạng thái của đơn hàng dựa trên mã đơn hàng (orderCode)")
    public Function<OrderStatusRequest, OrderStatusResponse> getOrderStatus(OrderRepository orderRepository) {
        return request -> {
            String code = request.orderCode();
            // Nếu người dùng nhập #DH001, ta có thể bỏ dấu # đi nếu cần
            if (code != null && code.startsWith("#")) {
                code = code.substring(1);
            }
            return orderRepository.findByOrderCode(code)
                    .map(order -> new OrderStatusResponse(order.getStatus(), "Đã tìm thấy đơn hàng"))
                    .orElse(new OrderStatusResponse("NOT_FOUND", "Không tìm thấy đơn hàng với mã: " + code));
        };
    }
}
