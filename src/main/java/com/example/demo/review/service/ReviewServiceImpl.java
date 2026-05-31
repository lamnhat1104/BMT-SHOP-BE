package com.example.demo.review.service;

import com.example.demo.review.dto.ReviewResponse;
import com.example.demo.review.entity.Review;
import com.example.demo.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;

    @Override
    public List<ReviewResponse> getAllReviews(Boolean showHidden) {
        return reviewRepository.findAll().stream()
                .filter(r -> (showHidden != null && showHidden) || (r.getIsActive() == null || r.getIsActive()))
                .map(ReviewResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteReview(Integer id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Đánh giá không tồn tại"));
        review.setIsActive(review.getIsActive() == null || !review.getIsActive());
        reviewRepository.save(review);
    }
}
