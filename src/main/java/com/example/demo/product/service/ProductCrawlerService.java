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
                    List<String> detailColors = new ArrayList<>();
                    List<String> detailSizes = new ArrayList<>();
                    java.util.Map<String, List<String>> colorImages = new java.util.HashMap<>();
                    java.util.Map<String, String> colorMainImage = new java.util.HashMap<>();
                    java.util.Map<String, Double> colorPriceMap = new java.util.HashMap<>();
                    String activeColor = "";
                    String crawledDescription = "";

                    if (!detailUrl.isEmpty()) {
                        try {
                            log.info("Đang truy cập trang chi tiết để cào ảnh và biến thể: {}", detailUrl);
                            Thread.sleep(300); // Tránh spam website mục tiêu
                            Document detailDoc = Jsoup.connect(detailUrl)
                                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                                    .header("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7")
                                    .timeout(15000)
                                    .get();

                            // 1. Cào các ảnh trong swiper hoặc gallery chính của sản phẩm
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

                            // 2. Cào danh sách màu sắc và cào trang riêng cho mỗi màu sắc (nếu có link) để tránh trộn ảnh
                            Elements colorElements = detailDoc.select(".reprelst bt, .reprelst .item, .nhom__01_mau");
                            for (Element colorEl : colorElements) {
                                String colorName = colorEl.select("img").attr("alt").trim();
                                if (colorName.isEmpty()) {
                                    colorName = colorEl.select(".rname").text().trim();
                                }
                                if (colorName.isEmpty()) {
                                    colorName = colorEl.text().trim();
                                }
                                
                                String lowerColor = colorName.toLowerCase();
                                if (lowerColor.isEmpty() || 
                                    lowerColor.contains("bảo hành") || 
                                    lowerColor.contains("căng") || 
                                    lowerColor.contains("đan") || 
                                    lowerColor.contains("khung") || 
                                    lowerColor.contains("chưa") || 
                                    lowerColor.contains("sẵn") || 
                                    lowerColor.contains("size") || 
                                    lowerColor.contains("kích") || 
                                    lowerColor.contains("trọng") || 
                                    lowerColor.matches("\\d+")) {
                                    continue; // Bỏ qua các thuộc tính không phải màu sắc (bảo hành, đan dây, kích thước...)
                                }
                                
                                if (!colorName.isEmpty()) {
                                    if (!detailColors.contains(colorName)) {
                                        detailColors.add(colorName);
                                    }
                                    
                                    // Lấy link cụ thể của màu sắc này để cào bộ ảnh riêng biệt
                                    String colorLink = colorEl.attr("abs:href");
                                    if (colorLink.isEmpty() && colorEl.tagName().equalsIgnoreCase("a")) {
                                        colorLink = colorEl.attr("abs:href");
                                    }
                                    if (colorLink.isEmpty()) {
                                        colorLink = colorEl.select("a").attr("abs:href");
                                    }
                                    
                                    List<String> imgs = new ArrayList<>();
                                    Document colorDoc = null;
                                    
                                    if (!colorLink.isEmpty() && !colorLink.equals(detailUrl)) {
                                        try {
                                            log.info("Cào trang riêng cho màu {}: {}", colorName, colorLink);
                                            Thread.sleep(200);
                                            colorDoc = Jsoup.connect(colorLink)
                                                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                                                    .header("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7")
                                                    .timeout(10000)
                                                    .get();
                                        } catch (Exception e) {
                                            log.warn("Không thể tải link màu sắc {}: {}", colorName, e.getMessage());
                                        }
                                    }
                                    
                                    if (colorDoc == null) {
                                        colorDoc = detailDoc;
                                    }
                                    
                                    // Cào bộ sưu tập ảnh từ trang của màu sắc đó
                                    Elements imgsEl = colorDoc.select(".gallery-thumbs img, .product-image-block img, .slider-big-video img, .bk-product-image, .gallery img, .product-gallery img");
                                    for (Element img : imgsEl) {
                                        String imgUrl = img.attr("abs:data-src");
                                        if (imgUrl.isEmpty() || imgUrl.startsWith("data:")) {
                                            imgUrl = img.attr("abs:src");
                                        }
                                        if (imgUrl.isEmpty() || imgUrl.startsWith("data:")) {
                                            imgUrl = img.attr("abs:data-original");
                                        }
                                        if (!imgUrl.isEmpty() && !imgUrl.startsWith("data:") && !imgs.contains(imgUrl)) {
                                            if (!imgUrl.contains("favicon") && !imgUrl.contains("logo") && !imgUrl.contains("icon")) {
                                                imgs.add(imgUrl);
                                            }
                                        }
                                    }
                                    
                                    // Lấy ảnh đại diện nhỏ làm fallback
                                    String variantImgUrl = colorEl.select("img").attr("abs:src");
                                    if (variantImgUrl.isEmpty() || variantImgUrl.startsWith("data:")) {
                                        variantImgUrl = colorEl.select("img").attr("abs:data-src");
                                    }
                                    if (!variantImgUrl.isEmpty() && !variantImgUrl.startsWith("data:")) {
                                        variantImgUrl = variantImgUrl.replace("/img/50x50/", "/")
                                                                     .replace("/img/300x300/", "/")
                                                                     .replace("/img/60x60/", "/")
                                                                     .replace("/img/150x150/", "/");
                                        if (!imgs.contains(variantImgUrl)) {
                                            imgs.add(0, variantImgUrl);
                                        }
                                        colorMainImage.put(colorName, variantImgUrl);
                                    } else if (!imgs.isEmpty()) {
                                        colorMainImage.put(colorName, imgs.get(0));
                                    }
                                    
                                    colorImages.put(colorName, imgs);
                                    
                                    // Lấy giá riêng của màu sắc này
                                    String variantPriceText = colorEl.select(".price").text().replaceAll("[^0-9]", "").trim();
                                    if (!variantPriceText.isEmpty()) {
                                        try {
                                            colorPriceMap.put(colorName, Double.parseDouble(variantPriceText));
                                        } catch (Exception e) {
                                            // ignore
                                        }
                                    }
                                }
                            }

                            // Tìm màu sắc đang được chọn mặc định (active)
                            Element activeColorEl = detailDoc.select(".reprelst bt.active, .reprelst .item.active, .nhom__01_mau.active").first();
                            if (activeColorEl != null) {
                                activeColor = activeColorEl.select("img").attr("alt").trim();
                                if (activeColor.isEmpty()) {
                                    activeColor = activeColorEl.select(".rname").text().trim();
                                }
                            } else if (!detailColors.isEmpty()) {
                                activeColor = detailColors.get(0);
                            }

                            // 3. Cào danh sách kích thước (Size)
                            Elements sizeElements = detailDoc.select(".swatch-element, .div_chon_size .swatch-element");
                            for (Element sizeEl : sizeElements) {
                                String sizeVal = sizeEl.attr("data-value").trim();
                                if (sizeVal.isEmpty()) {
                                    sizeVal = sizeEl.select(".ten_size").text().trim();
                                }
                                if (!sizeVal.isEmpty() && !detailSizes.contains(sizeVal)) {
                                    detailSizes.add(sizeVal);
                                }
                            }

                            // 4. Cào mô tả sản phẩm thực tế từ trang chi tiết
                            Element descEl = detailDoc.select(".product-description, .content-product, .product-content, .tab-content, .description, #tab-description, div[class*=description]").first();
                            if (descEl != null) {
                                crawledDescription = descEl.text().trim();
                            }
                        } catch (Exception e) {
                            log.error("Lỗi khi cào chi tiết sản phẩm từ URL {}: {}", detailUrl, e.getMessage());
                        }
                    }

                    // Làm sạch mô tả: loại bỏ nếu trống hoặc trùng lặp với tên
                    String sanitizedDescription = crawledDescription;
                    if (sanitizedDescription.isEmpty() || 
                        sanitizedDescription.equalsIgnoreCase(name) || 
                        (sanitizedDescription.startsWith(name) && sanitizedDescription.length() <= name.length() + 10)) {
                        sanitizedDescription = "";
                    }
                    
                    String description = sanitizedDescription;
                    if (description.isEmpty()) {
                        description = "Sản phẩm cầu lông chính hãng.";
                    }

                    // 6. Lưu sản phẩm vào DB để có ID
                    Product product = Product.builder()
                            .categoryId(category.getId())
                            .name(name)
                            .description(description)
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

                    // 8. Tạo biến thể mẫu tương ứng
                    List<ProductVariant> variants = new ArrayList<>();
                    
                    if (!detailColors.isEmpty() || !detailSizes.isEmpty()) {
                        log.info("Tạo biến thể động từ dữ liệu cào: {} màu, {} size", detailColors.size(), detailSizes.size());
                        
                        if (!detailColors.isEmpty() && !detailSizes.isEmpty()) {
                            // Có cả màu sắc và kích cỡ
                            for (String color : detailColors) {
                                for (String size : detailSizes) {
                                    double vPrice = colorPriceMap.getOrDefault(color, product.getPrice());
                                    variants.add(ProductVariant.builder()
                                            .productId(product.getId())
                                            .size(size)
                                            .color(color)
                                            .price(vPrice)
                                            .stock(15)
                                            .sku(generateSku(product.getName(), color, size))
                                            .images(new ArrayList<>())
                                            .build());
                                }
                            }
                        } else if (!detailColors.isEmpty()) {
                            // Chỉ có màu sắc
                            for (String color : detailColors) {
                                double vPrice = colorPriceMap.getOrDefault(color, product.getPrice());
                                variants.add(ProductVariant.builder()
                                        .productId(product.getId())
                                        .color(color)
                                        .price(vPrice)
                                        .stock(15)
                                        .sku(generateSku(product.getName(), color, null))
                                        .images(new ArrayList<>())
                                        .build());
                            }
                        } else {
                            // Chỉ có kích cỡ
                            for (String size : detailSizes) {
                                variants.add(ProductVariant.builder()
                                        .productId(product.getId())
                                        .size(size)
                                        .price(product.getPrice())
                                        .stock(15)
                                        .sku(generateSku(product.getName(), null, size))
                                        .images(new ArrayList<>())
                                        .build());
                            }
                        }
                    } else {
                        // Fallback sang logic cũ nếu không cào được thuộc tính động nào
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
                                        .sku(generateSku(product.getName(), "Tiêu chuẩn", size))
                                        .images(new ArrayList<>())
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
                                    .sku(generateSku(product.getName(), null, "3UG5"))
                                    .images(new ArrayList<>())
                                    .build());
                            variants.add(ProductVariant.builder()
                                    .productId(product.getId())
                                    .weight("4U")
                                    .grip("G5")
                                    .price(product.getPrice() - 50000.0 > 0 ? product.getPrice() - 50000.0 : product.getPrice())
                                    .stock(25)
                                    .sku(generateSku(product.getName(), null, "4UG5"))
                                    .images(new ArrayList<>())
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
                                        .sku(generateSku(product.getName(), color, null))
                                        .images(new ArrayList<>())
                                        .build());
                            }
                        }
                    }

                    // Gán hình ảnh cho từng biến thể một cách riêng biệt (tránh bị trùng ảnh)
                    for (ProductVariant var : variants) {
                        String color = var.getColor();
                        List<ProductImage> varImages = new ArrayList<>();
                        List<String> imgUrls = null;

                        if (color != null && colorImages.containsKey(color)) {
                            imgUrls = colorImages.get(color);
                        }

                        if ((imgUrls == null || imgUrls.isEmpty()) && !detailImages.isEmpty()) {
                            imgUrls = detailImages;
                        }

                        if (imgUrls != null && !imgUrls.isEmpty()) {
                            boolean isFirst = true;
                            for (String imgUrl : imgUrls) {
                                varImages.add(ProductImage.builder()
                                        .productId(product.getId())
                                        .imageUrl(imgUrl)
                                        .isMain(isFirst)
                                        .color(color)
                                        .build());
                                isFirst = false;
                            }
                        } else {
                            varImages.add(ProductImage.builder()
                                    .productId(product.getId())
                                    .imageUrl(product.getImageUrl())
                                    .isMain(true)
                                    .color(color)
                                    .build());
                        }
                        var.setImages(varImages);
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

    private String generateSku(String productName, String color, String size) {
        String cleanName = productName.replaceAll("[^a-zA-Z0-9-]", "-");
        StringBuilder sku = new StringBuilder(cleanName);
        if (color != null && !color.isEmpty()) {
            sku.append("-").append(color.replaceAll("[^a-zA-Z0-9-]", "-").toUpperCase());
        }
        if (size != null && !size.isEmpty()) {
            sku.append("-").append(size.replaceAll("[^a-zA-Z0-9-]", "-").toUpperCase());
        }
        return sku.toString().replaceAll("-+", "-");
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
