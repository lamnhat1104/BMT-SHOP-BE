package com.example.demo.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSaveRequest {
    
    private Integer categoryId;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    private String description;

    @NotBlank(message = "Thương hiệu không được để trống")
    private String brand;

    @NotNull(message = "Giá sản phẩm không được để trống")
    @Min(value = 0, message = "Giá sản phẩm không thể nhỏ hơn 0")
    private Double price;

    @NotNull(message = "Số lượng trong kho không được để trống")
    @Min(value = 0, message = "Số lượng trong kho không thể nhỏ hơn 0")
    private Integer stock;

    @Min(value = 0, message = "Khuyến mãi không thể nhỏ hơn 0")
    private Integer discountPercent;

    private String imageUrl;

    private Integer quantity;

    private Boolean isFeatured;

    private String status;
}
