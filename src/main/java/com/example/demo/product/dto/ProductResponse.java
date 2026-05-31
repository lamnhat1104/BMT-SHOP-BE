package com.example.demo.product.dto;

import com.example.demo.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Integer id;
    private Integer categoryId;
    private String name;
    private String description;
    private String brand;
    private Double price;
    private Integer stock;
    private Integer discountPercent;
    private String imageUrl;
    private Integer quantity;
    private Boolean isFeatured;
    private String status;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductResponse fromEntity(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .categoryId(product.getCategoryId())
                .name(product.getName())
                .description(product.getDescription())
                .brand(product.getBrand())
                .price(product.getPrice())
                .stock(product.getStock())
                .discountPercent(product.getDiscountPercent())
                .imageUrl(product.getImageUrl())
                .quantity(product.getQuantity())
                .isFeatured(product.getIsFeatured())
                .status(product.getStatus())
                .isDeleted(product.getIsDeleted())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}

