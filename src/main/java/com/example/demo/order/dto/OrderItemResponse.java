package com.example.demo.order.dto;

import com.example.demo.order.entity.OrderItem;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderItemResponse {
    private Integer id;
    private Integer productId;
    private String name;
    private Double price;
    private String thumbnail;
    private String brand;
    private Integer quantity;
    private String details;

    public static OrderItemResponse fromEntity(OrderItem item) {
        String name = item.getProduct().getName();
        String brand = "Unknown Brand";
        if (name != null) {
            String upper = name.toUpperCase();
            if (upper.contains("YONEX")) brand = "YONEX";
            else if (upper.contains("LINING")) brand = "LINING";
            else if (upper.contains("VICTOR")) brand = "VICTOR";
            else if (upper.contains("MIZUNO")) brand = "MIZUNO";
        }
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .name(name)
                .price(item.getPrice())
                .thumbnail(item.getProduct().getImageUrl())
                .brand(brand)
                .quantity(item.getQuantity())
                .details(item.getDetails())
                .build();
    }
}
