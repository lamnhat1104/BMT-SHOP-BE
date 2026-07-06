package com.example.demo.product.service;

import com.example.demo.product.dto.ProductResponse;
import com.example.demo.product.dto.ProductSaveRequest;
import com.example.demo.product.entity.Product;
import com.example.demo.product.entity.ProductVariant;
import com.example.demo.product.entity.ProductImage;
import com.example.demo.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public List<ProductResponse> getAllProducts(String keyword, String sort, String brand, Integer categoryId, Boolean showHidden, Double minPrice, Double maxPrice) {
        Sort jpaSort = Sort.unsorted();
        if ("newest".equalsIgnoreCase(sort)) {
            jpaSort = Sort.by(Sort.Direction.DESC, "createdAt");
        } else if ("price_asc".equalsIgnoreCase(sort)) {
            jpaSort = Sort.by(Sort.Direction.ASC, "price");
        } else if ("price_desc".equalsIgnoreCase(sort)) {
            jpaSort = Sort.by(Sort.Direction.DESC, "price");
        }

        List<Product> products = productRepository.filterProducts(keyword, brand, categoryId, minPrice, maxPrice, jpaSort);
        return products.stream()
                .filter(p -> (showHidden != null && showHidden) || (p.getIsDeleted() == null || !p.getIsDeleted()))
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> autocompleteSearch(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        org.springframework.data.domain.Pageable limit = org.springframework.data.domain.PageRequest.of(0, 5);
        return productRepository.autocompleteSearch(keyword.trim(), limit).stream()
                .filter(p -> (p.getIsDeleted() == null || !p.getIsDeleted()))
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
        List<ProductVariant> variants = new ArrayList<>();
        if (request.getVariants() != null) {
            for (ProductSaveRequest.VariantSaveRequest v : request.getVariants()) {
                variants.add(ProductVariant.builder()
                        .size(v.getSize())
                        .color(v.getColor())
                        .weight(v.getWeight())
                        .grip(v.getGrip())
                        .price(v.getPrice() != null ? v.getPrice() : request.getPrice())
                        .stock(v.getStock() != null ? v.getStock() : request.getStock())
                        .sku(v.getSku())
                        .build());
            }
        }

        List<ProductImage> images = new ArrayList<>();
        if (request.getImages() != null) {
            for (int i = 0; i < request.getImages().size(); i++) {
                String url = request.getImages().get(i);
                if (url != null && !url.isBlank()) {
                    images.add(ProductImage.builder()
                            .imageUrl(url)
                            .isMain(i == 0)
                            .build());
                }
            }
        }

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
                .variants(variants)
                .images(images)
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

        // Update variants
        if (product.getVariants() == null) {
            product.setVariants(new ArrayList<>());
        }
        product.getVariants().clear();
        if (request.getVariants() != null) {
            for (ProductSaveRequest.VariantSaveRequest v : request.getVariants()) {
                product.getVariants().add(ProductVariant.builder()
                        .productId(product.getId())
                        .size(v.getSize())
                        .color(v.getColor())
                        .weight(v.getWeight())
                        .grip(v.getGrip())
                        .price(v.getPrice() != null ? v.getPrice() : request.getPrice())
                        .stock(v.getStock() != null ? v.getStock() : request.getStock())
                        .sku(v.getSku())
                        .build());
            }
        }

        // Update images
        if (product.getImages() == null) {
            product.setImages(new ArrayList<>());
        }
        product.getImages().clear();
        if (request.getImages() != null) {
            for (int i = 0; i < request.getImages().size(); i++) {
                String url = request.getImages().get(i);
                if (url != null && !url.isBlank()) {
                    product.getImages().add(ProductImage.builder()
                            .productId(product.getId())
                            .imageUrl(url)
                            .isMain(i == 0)
                            .build());
                }
            }
        }

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
