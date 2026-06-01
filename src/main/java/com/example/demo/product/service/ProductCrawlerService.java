package com.example.demo.product.service;

import com.example.demo.category.entity.Category;
import com.example.demo.category.repository.CategoryRepository;
import com.example.demo.product.entity.Product;
import com.example.demo.product.entity.ProductImage;
import com.example.demo.product.entity.ProductVariant;
import com.example.demo.product.repository.ProductRepository;
import com.example.demo.product.repository.ProductImageRepository;
import com.example.demo.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCrawlerService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductVariantRepository productVariantRepository;

    @Transactional
    public int crawlProducts(String targetUrl, String categoryName) {
        try {
            log.info("Bắt đầu kết nối và cào dữ liệu từ URL: {}", targetUrl);
            
            // Gửi request giả lập một trình duyệt thông thường
            Document doc = Jsoup.connect(targetUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7")
                    .timeout(15000)
                    .get();

            // Tìm và xử lý Category trong Database (hoặc tạo mới nếu chưa tồn tại)
            Category category = categoryRepository.findByName(categoryName)
                    .orElseGet(() -> {
                        log.info("Danh mục '{}' chưa tồn tại. Đang tiến hành tạo mới...", categoryName);
                        Category newCat = Category.builder()
                                .name(categoryName)
                                .image("")
                                .isActive(true)
                                .build();
                        return categoryRepository.save(newCat);
                    });

            // Tìm danh sách các thẻ bao quanh sản phẩm
            // Sử dụng nhiều selectors phổ biến để đảm bảo tính tương thích cao nhất
            Elements productElements = doc.select(".item_product_main, .product-item, .product-loop-in, .item-product, .product-grid .product");
            
            if (productElements.isEmpty()) {
                // Thử thêm một số selectors khác nếu danh sách rỗng
                productElements = doc.select("div[class*=item_product], div[class*=product-item], div[class*=product-loop], div[class*=item-product]");
            }

            log.info("Tìm thấy {} thẻ sản phẩm trên giao diện HTML.", productElements.size());

            int count = 0;

            for (Element el : productElements) {
                try {
                    // 1. Trích xuất tên sản phẩm
                    String name = el.select(".product-title a, .product-name a, .title-product, h3, .product-title").text().trim();
                    if (name.isEmpty()) {
                        // Thử lấy thuộc tính title của ảnh hoặc thẻ chứa
                        name = el.select("img").attr("alt").trim();
                    }
                    if (name.isEmpty()) continue;

                    // 2. Trích xuất giá sản phẩm
                    String priceText = el.select(".product-price, .price-new, .price, .product-price-new").text()
                            .replaceAll("[^0-9]", "").trim();
                    double price = 0.0;
                    if (!priceText.isEmpty()) {
                        try {
                            price = Double.parseDouble(priceText);
                        } catch (NumberFormatException e) {
                            price = 0.0;
                        }
                    }

                    // 3. Trích xuất hình ảnh sản phẩm chính (lấy đường dẫn ảnh tuyệt đối)
                    String imageUrl = el.select("img").attr("abs:data-src");
                    if (imageUrl.isEmpty() || imageUrl.startsWith("data:")) {
                        imageUrl = el.select("img").attr("abs:src");
                    }
                    if (imageUrl.isEmpty() || imageUrl.startsWith("data:")) {
                        imageUrl = el.select("img").attr("abs:data-original");
                    }

                    // 4. Nhận diện thương hiệu (Brand) dựa trên tiêu đề sản phẩm
                    String brand = detectBrand(name);

                    // 5. Trích xuất đường dẫn chi tiết để cào album ảnh
                    String detailUrl = "";
                    Element detailLinkEl = el.select(".product-title a, .product-name a, a").first();
                    if (detailLinkEl != null) {
                        detailUrl = detailLinkEl.attr("abs:href");
                    }

                    List<String> detailImages = new ArrayList<>();
                    if (!detailUrl.isEmpty()) {
                        try {
                            log.info("Đang truy cập trang chi tiết để cào ảnh: {}", detailUrl);
                            Thread.sleep(300); // Tránh spam website mục tiêu
                            Document detailDoc = Jsoup.connect(detailUrl)
                                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                                    .header("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7")
                                    .timeout(15000)
                                    .get();

                            // Lấy các ảnh trong swiper hoặc gallery chính của sản phẩm (tránh rò rỉ ảnh của sản phẩm gợi ý/liên quan)
                            Elements galleryImgElements = detailDoc.select(".gallery-thumbs img, .product-image-block img, .slider-big-video img, .bk-product-image");
                            for (Element img : galleryImgElements) {
                                String imgUrl = img.attr("abs:data-src");
                                if (imgUrl.isEmpty() || imgUrl.startsWith("data:")) {
                                    imgUrl = img.attr("abs:src");
                                }
                                if (imgUrl.isEmpty() || imgUrl.startsWith("data:")) {
                                    imgUrl = img.attr("abs:data-original");
                                }
                                if (!imgUrl.isEmpty() && !imgUrl.startsWith("data:") && !detailImages.contains(imgUrl)) {
                                    // Loại bỏ các logo hoặc icon nhỏ không liên quan
                                    if (!imgUrl.contains("favicon") && !imgUrl.contains("logo") && !imgUrl.contains("icon")) {
                                        detailImages.add(imgUrl);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.error("Lỗi khi cào chi tiết sản phẩm từ URL {}: {}", detailUrl, e.getMessage());
                        }
                    }

                    // 6. Lưu sản phẩm vào DB để có ID
                    Product product = Product.builder()
                            .categoryId(category.getId())
                            .name(name)
                            .description(name + " chính hãng - Chất lượng cao, hỗ trợ tối đa cho người chơi.")
                            .brand(brand)
                            .price(price > 0 ? price : 1000000.0) // Nếu giá bằng 0 (Liên hệ) thì mặc định là 1.000.000đ
                            .stock(50) // Số lượng tồn kho ban đầu
                            .discountPercent(0)
                            .imageUrl(imageUrl.isEmpty() ? "/placeholder_racket.png" : imageUrl)
                            .quantity(1)
                            .isFeatured(false)
                            .status("available")
                            .isDeleted(false)
                            .build();

                    product = productRepository.save(product);

                    // 7. Lưu ảnh chính và album ảnh phụ
                    // Lưu ảnh chính trước
                    ProductImage mainImage = ProductImage.builder()
                            .productId(product.getId())
                            .imageUrl(product.getImageUrl())
                            .isMain(true)
                            .build();
                    productImageRepository.save(mainImage);

                    // Lưu các ảnh phụ chi tiết
                    for (String detailImgUrl : detailImages) {
                        if (detailImgUrl.equals(product.getImageUrl())) {
                            continue; // Tránh trùng lặp ảnh chính
                        }
                        ProductImage secImage = ProductImage.builder()
                                .productId(product.getId())
                                .imageUrl(detailImgUrl)
                                .isMain(false)
                                .build();
                        productImageRepository.save(secImage);
                    }

                    // 8. Tạo biến thể mẫu tương ứng
                    List<ProductVariant> variants = new ArrayList<>();
                    String lowerCatName = categoryName.toLowerCase();
                    String lowerName = name.toLowerCase();

                    if (lowerCatName.contains("giày") || lowerCatName.contains("giay") || lowerName.contains("giày") || lowerName.contains("giay")) {
                        // Tạo biến thể Size (39, 40, 41, 42, 43)
                        String[] sizes = {"39", "40", "41", "42", "43"};
                        for (String size : sizes) {
                            variants.add(ProductVariant.builder()
                                    .productId(product.getId())
                                    .size(size)
                                    .color("Tiêu chuẩn")
                                    .price(product.getPrice())
                                    .stock(10)
                                    .sku(product.getName().replaceAll("[^a-zA-Z0-9-]", "-") + "-SIZE-" + size)
                                    .build());
                        }
                    } else if (lowerCatName.contains("vợt") || lowerCatName.contains("vot") || lowerName.contains("vợt") || lowerName.contains("vot")) {
                        // Tạo biến thể Weight/Grip (3U/G5, 4U/G5)
                        variants.add(ProductVariant.builder()
                                .productId(product.getId())
                                .weight("3U")
                                .grip("G5")
                                .price(product.getPrice())
                                .stock(25)
                                .sku(product.getName().replaceAll("[^a-zA-Z0-9-]", "-") + "-3UG5")
                                .build());
                        variants.add(ProductVariant.builder()
                                .productId(product.getId())
                                .weight("4U")
                                .grip("G5")
                                .price(product.getPrice() - 50000.0 > 0 ? product.getPrice() - 50000.0 : product.getPrice())
                                .stock(25)
                                .sku(product.getName().replaceAll("[^a-zA-Z0-9-]", "-") + "-4UG5")
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
                                    .sku(product.getName().replaceAll("[^a-zA-Z0-9-]", "-") + "-" + color.toUpperCase())
                                    .build());
                        }
                    }
                    productVariantRepository.saveAll(variants);

                    count++;

                } catch (Exception e) {
                    log.error("Lỗi khi phân tích thẻ sản phẩm cụ thể: {}", e.getMessage());
                }
            }

            return count;

        } catch (IOException e) {
            log.error("Lỗi kết nối I/O khi cào dữ liệu: {}", e.getMessage());
            throw new RuntimeException("Lỗi kết nối tới trang web mục tiêu: " + e.getMessage());
        }
    }

    private String detectBrand(String productName) {
        String lowerName = productName.toLowerCase();
        if (lowerName.contains("yonex")) {
            return "Yonex";
        } else if (lowerName.contains("lining") || lowerName.contains("li-ning")) {
            return "Lining";
        } else if (lowerName.contains("victor")) {
            return "Victor";
        } else if (lowerName.contains("mizuno")) {
            return "Mizuno";
        } else if (lowerName.contains("kumpoo")) {
            return "Kumpoo";
        } else if (lowerName.contains("felet")) {
            return "Felet";
        } else if (lowerName.contains("apacs")) {
            return "Apacs";
        } else if (lowerName.contains("proace")) {
            return "Proace";
        } else if (lowerName.contains("vnb")) {
            return "VNB";
        } else {
            // Mặc định lấy từ đầu tiên của tên sản phẩm làm thương hiệu
            String[] words = productName.split("\\s+");
            if (words.length > 0 && words[0].length() > 1) {
                return words[0];
            }
            return "Khác";
        }
    }
}
