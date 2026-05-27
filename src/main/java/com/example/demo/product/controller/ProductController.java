package com.example.demo.product.controller;

import com.example.demo.product.dto.ProductResponse;
import com.example.demo.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts(
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Integer categoryId
    ) {
        return ResponseEntity.ok(productService.getAllProducts(sort, brand, categoryId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Integer id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }
}
