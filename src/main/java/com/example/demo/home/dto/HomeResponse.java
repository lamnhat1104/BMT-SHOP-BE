package com.example.demo.home.dto;

import com.example.demo.category.dto.CategoryResponse;
import com.example.demo.product.dto.ProductResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeResponse {
    private List<CategoryResponse> categories;
    private List<ProductResponse> featuredProducts;
    private List<ProductResponse> newArrivals;
    private List<ProductResponse> saleProducts;
    private List<ProductResponse> bestSellers;
    private List<String> topBrands;
}
