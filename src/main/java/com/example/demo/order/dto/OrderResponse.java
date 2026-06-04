package com.example.demo.order.dto;

import com.example.demo.order.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Integer id;
    private String orderCode;
    private Integer userId;
    private String fullName;
    private String phone;
    private String address;
    private String notes;
    private String paymentMethod;
    private String paymentStatus;
    private String status;
    private Double totalAmount;
    private String paymentUrl;
    private LocalDateTime createdAt;
    private List<OrderDetailResponse> items;

    public static OrderResponse fromEntity(Order order) {
        if (order == null) return null;
        
        List<OrderDetailResponse> listItems = Collections.emptyList();
        if (order.getOrderDetails() != null) {
            listItems = order.getOrderDetails().stream()
                    .map(OrderDetailResponse::fromEntity)
                    .collect(Collectors.toList());
        }

        return OrderResponse.builder()
            .id(order.getId())
            .orderCode(order.getOrderCode())
            .userId(order.getUserId())
            .fullName(order.getReceiverName())
            .phone(order.getReceiverPhone())
            .address(order.getShippingAddress())
            .notes(order.getNotes())
            .paymentMethod(order.getPaymentMethod())
            .paymentStatus(order.getPaymentStatus())
            .status(order.getStatus())
            .totalAmount(order.getTotalPrice())
            .createdAt(order.getOrderDate())
            .items(listItems)
            .build();
    }
}
