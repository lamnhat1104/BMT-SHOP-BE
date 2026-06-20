package com.example.demo.product.service;

import com.example.demo.product.dto.ProductResponse;
import com.example.demo.product.dto.ProductSaveRequest;
import java.util.List;

public interface ProductService {
    List<ProductResponse> getAllProducts(String sort, String brand, Integer categoryId, Boolean showHidden, Double minPrice, Double maxPrice);
    default List<ProductResponse> getAllProducts() {
        return getAllProducts(null, null, null, false, null, null);
    }
    ProductResponse getProductById(Integer id);
    ProductResponse createProduct(ProductSaveRequest request);
    ProductResponse updateProduct(Integer id, ProductSaveRequest request);
    void deleteProduct(Integer id);
}
