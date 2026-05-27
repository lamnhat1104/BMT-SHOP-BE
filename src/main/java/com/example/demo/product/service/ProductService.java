package com.example.demo.product.service;

import com.example.demo.product.dto.ProductResponse;
import java.util.List;

public interface ProductService {
    List<ProductResponse> getAllProducts(String sort, String brand, Integer categoryId);
    ProductResponse getProductById(Integer id);
}
