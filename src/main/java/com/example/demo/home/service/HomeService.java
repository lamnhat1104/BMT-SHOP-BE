package com.example.demo.home.service;

import com.example.demo.category.dto.CategoryResponse;
import com.example.demo.category.repository.CategoryRepository;
import com.example.demo.home.dto.HomeResponse;
import com.example.demo.product.dto.ProductResponse;
import com.example.demo.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public HomeResponse getHomeData() {
        // 1. Categories
        List<CategoryResponse> categories = categoryRepository.findAll().stream()
                .filter(c -> c.getIsActive() == null || c.getIsActive())
                .map(CategoryResponse::fromEntity)
                .collect(Collectors.toList());

        // Limit for product lists
        Pageable limit8 = PageRequest.of(0, 8);

        // 2. Featured Products
        List<ProductResponse> featuredProducts = productRepository.findFeaturedProducts(limit8).stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());

        // 3. New Arrivals
        Pageable newArrivalsPageable = PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<ProductResponse> newArrivals = productRepository.findActiveProducts(newArrivalsPageable).stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());

        // 4. Sale Products
        List<ProductResponse> saleProducts = productRepository.findSaleProducts(limit8).stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());

        // 5. Best Sellers
        Pageable bestSellersPageable = PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "soldCount"));
        List<ProductResponse> bestSellers = productRepository.findActiveProducts(bestSellersPageable).stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());

        // 6. Top Brands
        List<String> topBrands = Arrays.asList("YONEX", "LINING", "VICTOR", "MIZUNO");

        return HomeResponse.builder()
                .categories(categories)
                .featuredProducts(featuredProducts)
                .newArrivals(newArrivals)
                .saleProducts(saleProducts)
                .bestSellers(bestSellers)
                .topBrands(topBrands)
                .build();
    }
}
