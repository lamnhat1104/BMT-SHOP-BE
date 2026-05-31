package com.example.demo.order.dto;

import com.example.demo.order.entity.OrderDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponse {
    private Integer id;
    private Integer productId;
    private String name;
    private String brand;
    private String details;
    private Integer quantity;
    private Double price;
    private String thumbnail;

    public static OrderDetailResponse fromEntity(OrderDetail detail) {
        if (detail == null) return null;
        return OrderDetailResponse.builder()
                .id(detail.getId())
                .productId(detail.getProductId())
                .name(detail.getProduct() != null ? detail.getProduct().getName() : "Sản phẩm đã xóa")
                .brand(detail.getProduct() != null ? detail.getProduct().getBrand() : "")
                .details(detail.getDetails() != null ? detail.getDetails() : "")
                .quantity(detail.getQuantity())
                .price(detail.getUnitPrice())
                .thumbnail(detail.getProduct() != null ? detail.getProduct().getImageUrl() : "")
                .build();
    }
}
