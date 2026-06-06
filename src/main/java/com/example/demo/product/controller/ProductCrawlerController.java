package com.example.demo.product.controller;

import com.example.demo.product.service.ProductCrawlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/crawler")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductCrawlerController {

    private final ProductCrawlerService productCrawlerService;

    @PostMapping("/run")
    public ResponseEntity<String> runCrawler(
            @RequestParam String url,
            @RequestParam String categoryName
    ) {
        try {
            int count = productCrawlerService.crawlProducts(url, categoryName);
            return ResponseEntity.ok("Cào dữ liệu thành công! Đã thêm " + count + " sản phẩm vào danh mục '" + categoryName + "' vào cơ sở dữ liệu.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi trong quá trình cào dữ liệu: " + e.getMessage());
        }
    }
}
