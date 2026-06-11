package com.example.demo.review.dto;

import com.example.demo.review.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Integer id;
    private Integer productId;
    private String productName;
    private Integer userId;
    private String userFullName;
    private Integer rating;
    private String comment;
    private String reply;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private java.util.List<String> imageUrls;

    public static ReviewResponse fromEntity(Review review) {
        if (review == null) {
            return null;
        }
        
        java.util.List<String> urls = new java.util.ArrayList<>();
        if (review.getImages() != null) {
            for (com.example.demo.review.entity.ReviewImage img : review.getImages()) {
                urls.add(img.getImageUrl());
            }
        }

        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProductId())
                .productName(review.getProduct() != null ? review.getProduct().getName() : "Sản phẩm đã bị xóa")
                .userId(review.getUserId())
                .userFullName(review.getUser() != null ? review.getUser().getFullName() : "Tài khoản đã xóa")
                .rating(review.getRating())
                .comment(review.getComment())
                .reply(review.getReply())
                .isActive(review.getIsActive())
                .createdAt(review.getCreatedAt())
                .imageUrls(urls)
                .build();
    }
}
