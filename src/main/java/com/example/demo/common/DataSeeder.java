package com.example.demo.common;

import com.example.demo.category.entity.Category;
import com.example.demo.category.repository.CategoryRepository;
import com.example.demo.product.entity.Product;
import com.example.demo.product.entity.ProductImage;
import com.example.demo.product.entity.ProductVariant;
import com.example.demo.product.repository.ProductImageRepository;
import com.example.demo.product.repository.ProductRepository;
import com.example.demo.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("Khởi động hệ thống: Đang kiểm tra dữ liệu biến thể và hình ảnh mẫu...");

        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            log.info("Chưa có sản phẩm nào trong cơ sở dữ liệu. Bỏ qua bước gieo dữ liệu biến thể.");
            return;
        }

        // Đếm xem đã có biến thể nào trong DB chưa
        long variantCount = productVariantRepository.count();
        long imageCount = productImageRepository.count();

        if (variantCount > 0 && imageCount > 0) {
            log.info("Đã tồn tại {} biến thể và {} hình ảnh trong cơ sở dữ liệu. Không cần gieo dữ liệu mới.", variantCount, imageCount);
            return;
        }

        log.info("Tiến hành tự động gieo biến thể và bộ sưu tập ảnh cho {} sản phẩm...", products.size());

        for (Product product : products) {
            // Lấy danh mục của sản phẩm
            Category category = categoryRepository.findById(product.getCategoryId()).orElse(null);
            String categoryName = (category != null) ? category.getName().toLowerCase() : "";

            // 1. Tạo hình ảnh bộ sưu tập mẫu (Gallery Images)
            List<ProductImage> galleryImages = new ArrayList<>();
            // Ảnh chính
            galleryImages.add(ProductImage.builder()
                    .productId(product.getId())
                    .imageUrl(product.getImageUrl() != null && !product.getImageUrl().isEmpty() ? product.getImageUrl() : "/placeholder_racket.png")
                    .isMain(true)
                    .build());

            // Ảnh phụ chi tiết
            if (categoryName.contains("giày") || categoryName.contains("giay")) {
                galleryImages.add(ProductImage.builder()
                        .productId(product.getId())
                        .imageUrl("https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600&auto=format&fit=crop&q=80")
                        .isMain(false)
                        .color("Trắng")
                        .build());
                galleryImages.add(ProductImage.builder()
                        .productId(product.getId())
                        .imageUrl("https://images.unsplash.com/photo-1600185365483-26d7a4cc7519?w=600&auto=format&fit=crop&q=80")
                        .isMain(false)
                        .color("Đen")
                        .build());
            } else if (categoryName.contains("vợt") || categoryName.contains("vot")) {
                galleryImages.add(ProductImage.builder()
                        .productId(product.getId())
                        .imageUrl("https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=600&auto=format&fit=crop&q=80")
                        .isMain(false)
                        .build());
                galleryImages.add(ProductImage.builder()
                        .productId(product.getId())
                        .imageUrl("https://images.unsplash.com/photo-1613918431208-67527b6f4348?w=600&auto=format&fit=crop&q=80")
                        .isMain(false)
                        .build());
            } else {
                galleryImages.add(ProductImage.builder()
                        .productId(product.getId())
                        .imageUrl("https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=600&auto=format&fit=crop&q=80")
                        .isMain(false)
                        .color("Đen")
                        .build());
                galleryImages.add(ProductImage.builder()
                        .productId(product.getId())
                        .imageUrl("https://images.unsplash.com/photo-1622560480605-d83c853bc5c3?w=600&auto=format&fit=crop&q=80")
                        .isMain(false)
                        .color("Xanh")
                        .build());
            }
            productImageRepository.saveAll(galleryImages);

            // 2. Tạo biến thể mẫu (Product Variants)
            List<ProductVariant> variants = new ArrayList<>();
            if (categoryName.contains("giày") || categoryName.contains("giay")) {
                // Tạo biến thể Size & Màu sắc
                variants.add(ProductVariant.builder()
                        .productId(product.getId())
                        .size("39")
                        .color("Trắng")
                        .price(product.getPrice())
                        .stock(10)
                        .sku(product.getName().replaceAll("\\s+", "-") + "-WHITE-39")
                        .build());
                variants.add(ProductVariant.builder()
                        .productId(product.getId())
                        .size("40")
                        .color("Trắng")
                        .price(product.getPrice())
                        .stock(10)
                        .sku(product.getName().replaceAll("\\s+", "-") + "-WHITE-40")
                        .build());
                variants.add(ProductVariant.builder()
                        .productId(product.getId())
                        .size("41")
                        .color("Đen")
                        .price(product.getPrice())
                        .stock(10)
                        .sku(product.getName().replaceAll("\\s+", "-") + "-BLACK-41")
                        .build());
                variants.add(ProductVariant.builder()
                        .productId(product.getId())
                        .size("42")
                        .color("Đen")
                        .price(product.getPrice())
                        .stock(10)
                        .sku(product.getName().replaceAll("\\s+", "-") + "-BLACK-42")
                        .build());
            } else if (categoryName.contains("vợt") || categoryName.contains("vot")) {
                // Tạo biến thể Weight/Grip (3U/G5, 4U/G5)
                variants.add(ProductVariant.builder()
                        .productId(product.getId())
                        .weight("3U")
                        .grip("G5")
                        .price(product.getPrice())
                        .stock(25)
                        .sku(product.getName().replaceAll("\\s+", "-") + "-3UG5")
                        .build());
                variants.add(ProductVariant.builder()
                        .productId(product.getId())
                        .weight("4U")
                        .grip("G5")
                        .price(product.getPrice() - 50000.0 > 0 ? product.getPrice() - 50000.0 : product.getPrice()) // 4U nhẹ hơn tí, có thể giảm giá nhẹ
                        .stock(25)
                        .sku(product.getName().replaceAll("\\s+", "-") + "-4UG5")
                        .build());
            } else {
                // Tạo biến thể Màu sắc
                String[] colors = {"Đen", "Xanh", "Đỏ"};
                for (String color : colors) {
                    variants.add(ProductVariant.builder()
                            .productId(product.getId())
                            .color(color)
                            .price(product.getPrice())
                            .stock(15)
                            .sku(product.getName().replaceAll("\\s+", "-") + "-" + color.toUpperCase())
                            .build());
                }
            }
            productVariantRepository.saveAll(variants);
        }

        log.info("Hoàn tất gieo dữ liệu biến thể và hình ảnh mẫu thành công!");
    }
}
