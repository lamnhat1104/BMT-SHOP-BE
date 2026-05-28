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
    private String name;
    private String description;
    private Double price;
    private Integer stock;
    private String imageUrl;
    private LocalDateTime createdAt;
    private BrandResponse brand;
    private CategoryResponse category;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class BrandResponse {
        private String name;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class CategoryResponse {
        private Integer id;
        private String name;
    }

    public static ProductResponse fromEntity(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .imageUrl(product.getImageUrl())
                .createdAt(product.getCreatedAt())
                .brand(product.getBrand() != null ? new BrandResponse(product.getBrand()) : null)
                .category(product.getCategoryId() != null ? new CategoryResponse(product.getCategoryId(), product.getCategoryName()) : null)
                .build();
    }
}
