package com.example.demo.config;

import com.example.demo.account.entity.User;
import com.example.demo.account.repository.UserRepository;
import com.example.demo.category.entity.Category;
import com.example.demo.category.repository.CategoryRepository;
import com.example.demo.product.entity.Product;
import com.example.demo.product.entity.ProductImage;
import com.example.demo.product.repository.ProductImageRepository;
import com.example.demo.product.repository.ProductRepository;
import com.example.demo.review.entity.ReviewImage;
import com.example.demo.review.repository.ReviewImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryMigrationService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    public String migrateAllImages() {
        int productCount = 0;
        int productImageCount = 0;
        int categoryCount = 0;
        int reviewCount = 0;
        int userCount = 0;

        // 1. Migrate Products
        List<Product> products = productRepository.findAll();
        for (Product product : products) {
            if (product.getImageUrl() != null && !product.getImageUrl().isEmpty() && !product.getImageUrl().contains("res.cloudinary.com")) {
                String newUrl = cloudinaryService.uploadFromUrl(product.getImageUrl());
                if (!newUrl.equals(product.getImageUrl())) {
                    product.setImageUrl(newUrl);
                    productRepository.save(product);
                    productCount++;
                }
            }
        }

        // 2. Migrate Product Images
        List<ProductImage> productImages = productImageRepository.findAll();
        for (ProductImage img : productImages) {
            if (img.getImageUrl() != null && !img.getImageUrl().isEmpty() && !img.getImageUrl().contains("res.cloudinary.com")) {
                String newUrl = cloudinaryService.uploadFromUrl(img.getImageUrl());
                if (!newUrl.equals(img.getImageUrl())) {
                    img.setImageUrl(newUrl);
                    productImageRepository.save(img);
                    productImageCount++;
                }
            }
        }

        // 3. Migrate Categories
        List<Category> categories = categoryRepository.findAll();
        for (Category cat : categories) {
            if (cat.getImage() != null && !cat.getImage().isEmpty() && !cat.getImage().contains("res.cloudinary.com")) {
                String newUrl = cloudinaryService.uploadFromUrl(cat.getImage());
                if (!newUrl.equals(cat.getImage())) {
                    cat.setImage(newUrl);
                    categoryRepository.save(cat);
                    categoryCount++;
                }
            }
        }

        // 4. Migrate Review Images
        List<ReviewImage> reviewImages = reviewImageRepository.findAll();
        for (ReviewImage img : reviewImages) {
            if (img.getImageUrl() != null && !img.getImageUrl().isEmpty() && !img.getImageUrl().contains("res.cloudinary.com")) {
                String newUrl = cloudinaryService.uploadFromUrl(img.getImageUrl());
                if (!newUrl.equals(img.getImageUrl())) {
                    img.setImageUrl(newUrl);
                    reviewImageRepository.save(img);
                    reviewCount++;
                }
            }
        }

        // 5. Migrate Users
        List<User> users = userRepository.findAll();
        for (User user : users) {
            if (user.getAvatar() != null && !user.getAvatar().isEmpty() && !user.getAvatar().contains("res.cloudinary.com")) {
                String newUrl = cloudinaryService.uploadFromUrl(user.getAvatar());
                if (!newUrl.equals(user.getAvatar())) {
                    user.setAvatar(newUrl);
                    userRepository.save(user);
                    userCount++;
                }
            }
        }

        String result = String.format("Migrate hoàn tất: %d Sản phẩm, %d Ảnh phụ SP, %d Danh mục, %d Ảnh Review, %d Avatar User.",
                productCount, productImageCount, categoryCount, reviewCount, userCount);
        log.info(result);
        return result;
    }
}
