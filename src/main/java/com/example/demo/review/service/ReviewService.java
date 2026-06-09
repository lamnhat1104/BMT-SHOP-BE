package com.example.demo.review.service;

import com.example.demo.review.dto.ReviewResponse;
import java.util.List;

public interface ReviewService {
    List<ReviewResponse> getAllReviews(Boolean showHidden);
    default List<ReviewResponse> getAllReviews() {
        return getAllReviews(false);
    }
    void deleteReview(Integer id);
    
    List<ReviewResponse> getReviewsByProductId(Integer productId);
    ReviewResponse createReview(Integer userId, com.example.demo.review.dto.ReviewRequest request, java.util.List<org.springframework.web.multipart.MultipartFile> files);
}
