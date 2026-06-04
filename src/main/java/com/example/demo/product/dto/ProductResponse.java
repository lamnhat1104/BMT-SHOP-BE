package com.example.demo.product.dto;

import com.example.demo.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
    private CategoryResponse category;
    private List<ImageResponse> images;
    private List<VariantResponse> variants;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class CategoryResponse {
        private Integer id;
        private String name;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ImageResponse {
        private Integer id;
        private String imageUrl;
        private Boolean isMain;
        private String color;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class VariantResponse {
        private Integer id;
        private String size;
        private String color;
        private String weight;
        private String grip;
        private Double price;
        private Integer stock;
        private String sku;
        private List<ImageResponse> images;
    }

    public static ProductResponse fromEntity(Product product) {
        if (product == null) return null;

        List<ImageResponse> uniqueImages = new java.util.ArrayList<>();
        java.util.Set<String> seenUrls = new java.util.HashSet<>();
        if (product.getVariants() != null) {
            for (com.example.demo.product.entity.ProductVariant v : product.getVariants()) {
                if (v.getImages() != null) {
                    for (com.example.demo.product.entity.ProductImage img : v.getImages()) {
                        if (!seenUrls.contains(img.getImageUrl())) {
                            seenUrls.add(img.getImageUrl());
                            uniqueImages.add(ImageResponse.builder()
                                    .id(img.getId())
                                    .imageUrl(img.getImageUrl())
                                    .isMain(img.getIsMain())
                                    .color(img.getColor())
                                    .build());
                        }
                    }
                }
            }
        }

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
                .category(product.getCategory() != null ? CategoryResponse.builder()
                        .id(product.getCategory().getId())
                        .name(product.getCategory().getName())
                        .build() : null)
                .images(uniqueImages)
                .variants(product.getVariants() != null ? product.getVariants().stream()
                        .map(var -> VariantResponse.builder()
                                .id(var.getId())
                                .size(var.getSize())
                                .color(var.getColor())
                                .weight(var.getWeight())
                                .grip(var.getGrip())
                                .price(var.getPrice())
                                .stock(var.getStock())
                                .sku(var.getSku())
                                .images(var.getImages() != null ? var.getImages().stream()
                                        .map(img -> ImageResponse.builder()
                                                .id(img.getId())
                                                .imageUrl(img.getImageUrl())
                                                .isMain(img.getIsMain())
                                                .color(img.getColor())
                                                .build())
                                        .collect(Collectors.toList()) : java.util.Collections.emptyList())
                                .build())
                        .collect(Collectors.toList()) : java.util.Collections.emptyList())
                .build();
    }
}
