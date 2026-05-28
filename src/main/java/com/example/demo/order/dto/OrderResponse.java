package com.example.demo.order.dto;

import com.example.demo.order.entity.Order;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class OrderResponse {
    private Integer id;
    private String orderCode;
    private String fullName;
    private String phone;
    private String address;
    private String notes;
    private String paymentMethod;
    private String status;
    private Double totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemResponse> items;

    public static OrderResponse fromEntity(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .fullName(order.getFullName())
                .phone(order.getPhone())
                .address(order.getAddress())
                .notes(order.getNotes())
                .paymentMethod(order.getPaymentMethod())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(order.getItems() == null ? List.of() : order.getItems().stream()
                        .map(OrderItemResponse::fromEntity)
                        .collect(Collectors.toList()))
                .build();
    }
}
