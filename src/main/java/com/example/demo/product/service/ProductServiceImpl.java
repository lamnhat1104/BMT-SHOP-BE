package com.example.demo.product.service;

import com.example.demo.product.dto.ProductResponse;
import com.example.demo.product.entity.Product;
import com.example.demo.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public List<ProductResponse> getAllProducts(String sort, String brand, Integer categoryId) {
        Sort jpaSort = Sort.unsorted();
        if ("newest".equalsIgnoreCase(sort)) {
            jpaSort = Sort.by(Sort.Direction.DESC, "createdAt");
        } else if ("price_asc".equalsIgnoreCase(sort)) {
            jpaSort = Sort.by(Sort.Direction.ASC, "price");
        } else if ("price_desc".equalsIgnoreCase(sort)) {
            jpaSort = Sort.by(Sort.Direction.DESC, "price");
        }

        return productRepository.filterProducts(brand, categoryId, jpaSort).stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public ProductResponse getProductById(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return ProductResponse.fromEntity(product);
    }
}
