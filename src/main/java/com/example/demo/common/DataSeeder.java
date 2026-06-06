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

            // 2. Tạo biến thể mẫu (Product Variants) và gán hình ảnh cho từng biến thể
            List<ProductVariant> variants = new ArrayList<>();
            String mainImgUrl = product.getImageUrl() != null && !product.getImageUrl().isEmpty() ? product.getImageUrl() : "/placeholder_racket.png";

            if (categoryName.contains("giày") || categoryName.contains("giay")) {
                // Biến thể màu Trắng
                ProductVariant white39 = ProductVariant.builder()
                        .productId(product.getId())
                        .size("39")
                        .color("Trắng")
                        .price(product.getPrice())
                        .stock(10)
                        .sku(product.getName().replaceAll("\\s+", "-") + "-WHITE-39")
                        .images(new ArrayList<>())
                        .build();
                white39.getImages().add(ProductImage.builder().productId(product.getId()).imageUrl(mainImgUrl).isMain(true).color("Trắng").build());
                white39.getImages().add(ProductImage.builder().productId(product.getId()).imageUrl("https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600&auto=format&fit=crop&q=80").isMain(false).color("Trắng").build());

                ProductVariant white40 = ProductVariant.builder()
                        .productId(product.getId())
                        .size("40")
                        .color("Trắng")
                        .price(product.getPrice())
                        .stock(10)
                        .sku(product.getName().replaceAll("\\s+", "-") + "-WHITE-40")
                        .images(new ArrayList<>())
                        .build();
                white40.getImages().add(ProductImage.builder().productId(product.getId()).imageUrl(mainImgUrl).isMain(true).color("Trắng").build());
                white40.getImages().add(ProductImage.builder().productId(product.getId()).imageUrl("https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600&auto=format&fit=crop&q=80").isMain(false).color("Trắng").build());

                // Biến thể màu Đen
                ProductVariant black41 = ProductVariant.builder()
                        .productId(product.getId())
                        .size("41")
                        .color("Đen")
                        .price(product.getPrice())
                        .stock(10)
                        .sku(product.getName().replaceAll("\\s+", "-") + "-BLACK-41")
                        .images(new ArrayList<>())
                        .build();
                black41.getImages().add(ProductImage.builder().productId(product.getId()).imageUrl(mainImgUrl).isMain(true).color("Đen").build());
                black41.getImages().add(ProductImage.builder().productId(product.getId()).imageUrl("https://images.unsplash.com/photo-1600185365483-26d7a4cc7519?w=600&auto=format&fit=crop&q=80").isMain(false).color("Đen").build());

                ProductVariant black42 = ProductVariant.builder()
                        .productId(product.getId())
                        .size("42")
                        .color("Đen")
                        .price(product.getPrice())
                        .stock(10)
                        .sku(product.getName().replaceAll("\\s+", "-") + "-BLACK-42")
                        .images(new ArrayList<>())
                        .build();
                black42.getImages().add(ProductImage.builder().productId(product.getId()).imageUrl(mainImgUrl).isMain(true).color("Đen").build());
                black42.getImages().add(ProductImage.builder().productId(product.getId()).imageUrl("https://images.unsplash.com/photo-1600185365483-26d7a4cc7519?w=600&auto=format&fit=crop&q=80").isMain(false).color("Đen").build());

                variants.add(white39);
                variants.add(white40);
                variants.add(black41);
                variants.add(black42);
            } else if (categoryName.contains("vợt") || categoryName.contains("vot")) {
                // Tạo biến thể Weight/Grip (3U/G5, 4U/G5)
                ProductVariant v3u = ProductVariant.builder()
                        .productId(product.getId())
                        .weight("3U")
                        .grip("G5")
                        .price(product.getPrice())
                        .stock(25)
                        .sku(product.getName().replaceAll("\\s+", "-") + "-3UG5")
                        .images(new ArrayList<>())
                        .build();
                v3u.getImages().add(ProductImage.builder().productId(product.getId()).imageUrl(mainImgUrl).isMain(true).build());
                v3u.getImages().add(ProductImage.builder().productId(product.getId()).imageUrl("https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=600&auto=format&fit=crop&q=80").isMain(false).build());

                ProductVariant v4u = ProductVariant.builder()
                        .productId(product.getId())
                        .weight("4U")
                        .grip("G5")
                        .price(product.getPrice() - 50000.0 > 0 ? product.getPrice() - 50000.0 : product.getPrice())
                        .stock(25)
                        .sku(product.getName().replaceAll("\\s+", "-") + "-4UG5")
                        .images(new ArrayList<>())
                        .build();
                v4u.getImages().add(ProductImage.builder().productId(product.getId()).imageUrl(mainImgUrl).isMain(true).build());
                v4u.getImages().add(ProductImage.builder().productId(product.getId()).imageUrl("https://images.unsplash.com/photo-1613918431208-67527b6f4348?w=600&auto=format&fit=crop&q=80").isMain(false).build());

                variants.add(v3u);
                variants.add(v4u);
            } else {
                // Tạo biến thể Màu sắc
                String[] colors = {"Đen", "Xanh", "Đỏ"};
                for (String color : colors) {
                    ProductVariant v = ProductVariant.builder()
                            .productId(product.getId())
                            .color(color)
                            .price(product.getPrice())
                            .stock(15)
                            .sku(product.getName().replaceAll("\\s+", "-") + "-" + color.toUpperCase())
                            .images(new ArrayList<>())
                            .build();
                    v.getImages().add(ProductImage.builder().productId(product.getId()).imageUrl(mainImgUrl).isMain(true).color(color).build());
                    
                    String detailColorUrl = "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=600&auto=format&fit=crop&q=80";
                    if ("Xanh".equalsIgnoreCase(color)) {
                        detailColorUrl = "https://images.unsplash.com/photo-1622560480605-d83c853bc5c3?w=600&auto=format&fit=crop&q=80";
                    }
                    v.getImages().add(ProductImage.builder().productId(product.getId()).imageUrl(detailColorUrl).isMain(false).color(color).build());
                    variants.add(v);
                }
            }
            productVariantRepository.saveAll(variants);
        }

        log.info("Hoàn tất gieo dữ liệu biến thể và hình ảnh mẫu thành công!");
    }
}
