package com.example.demo.product.service;

import com.example.demo.product.dto.ProductResponse;
import com.example.demo.product.dto.ProductSaveRequest;
import com.example.demo.product.entity.Product;
import com.example.demo.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public List<ProductResponse> getAllProducts(Boolean showHidden) {
        return productRepository.findAll().stream()
                .filter(p -> (showHidden != null && showHidden) || (p.getIsDeleted() == null || !p.getIsDeleted()))
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public ProductResponse getProductById(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return ProductResponse.fromEntity(product);
    }

    @Override
    public ProductResponse createProduct(ProductSaveRequest request) {
        Product product = Product.builder()
                .categoryId(request.getCategoryId())
                .name(request.getName())
                .description(request.getDescription())
                .brand(request.getBrand())
                .price(request.getPrice())
                .stock(request.getStock())
                .discountPercent(request.getDiscountPercent() != null ? request.getDiscountPercent() : 0)
                .imageUrl(request.getImageUrl())
                .quantity(request.getQuantity() != null ? request.getQuantity() : 1)
                .isFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false)
                .status(request.getStatus() != null ? request.getStatus() : "available")
                .isDeleted(false)
                .build();
        Product saved = productRepository.save(product);
        return ProductResponse.fromEntity(saved);
    }

    @Override
    public ProductResponse updateProduct(Integer id, ProductSaveRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setCategoryId(request.getCategoryId());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setBrand(request.getBrand());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setDiscountPercent(request.getDiscountPercent() != null ? request.getDiscountPercent() : 0);
        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            product.setImageUrl(request.getImageUrl());
        }
        product.setQuantity(request.getQuantity() != null ? request.getQuantity() : 1);
        product.setIsFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false);
        product.setStatus(request.getStatus() != null ? request.getStatus() : "available");
        Product saved = productRepository.save(product);
        return ProductResponse.fromEntity(saved);
    }

    @Override
    public void deleteProduct(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        boolean nextDeletedState = (product.getIsDeleted() == null || !product.getIsDeleted());
        product.setIsDeleted(nextDeletedState);
        product.setStatus(nextDeletedState ? "out_of_stock" : "available");
        productRepository.save(product);
    }
}
