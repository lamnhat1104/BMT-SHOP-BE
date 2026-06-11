package com.example.demo.review.service;

import com.example.demo.order.entity.Order;
import com.example.demo.order.repository.OrderRepository;
import com.example.demo.review.dto.ReviewRequest;
import com.example.demo.review.dto.ReviewResponse;
import com.example.demo.review.entity.Review;
import com.example.demo.review.entity.ReviewImage;
import com.example.demo.review.repository.ReviewImageRepository;
import com.example.demo.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final ReviewImageRepository reviewImageRepository;

    private final String UPLOAD_DIR = "uploads/reviews/";

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

    @Override
    public List<ReviewResponse> getReviewsByProductId(Integer productId) {
        return reviewRepository.findByProductIdAndIsActiveTrueOrderByCreatedAtDesc(productId)
                .stream()
                .map(ReviewResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReviewResponse createReview(Integer userId, ReviewRequest request, List<MultipartFile> files) {
        // userId might not be passed correctly if we use email, so let's check.
        // Actually, let's keep the parameter as userId, and the Controller will pass it.
        // Wait, Controller will just pass email and we can query here, or controller queries and passes userId.
        // Let's assume controller passes userId. So this method is fine.
        
        if (request.getOrderId() != null) {
            // 1. Kiểm tra user đã mua sản phẩm và nhận hàng thành công chưa
            List<Order> completedOrders = orderRepository.findByUserIdAndStatus(userId, "completed");
            boolean hasBought = completedOrders.stream().anyMatch(order -> 
                order.getId().equals(request.getOrderId()) &&
                order.getOrderDetails().stream().anyMatch(od -> od.getProductId().equals(request.getProductId()))
            );

            if (!hasBought) {
                throw new RuntimeException("Bạn chỉ có thể đánh giá sản phẩm sau khi đã mua và nhận hàng thành công từ đơn hàng này.");
            }

            // 2. Kiểm tra xem đã review chưa cho đơn hàng này
            if (reviewRepository.existsByOrderIdAndProductId(request.getOrderId(), request.getProductId())) {
                throw new RuntimeException("Bạn đã đánh giá sản phẩm này trong đơn hàng này rồi.");
            }
        }

        // 3. Tạo Review
        Review review = Review.builder()
                .userId(userId)
                .productId(request.getProductId())
                .orderId(request.getOrderId())
                .rating(request.getRating())
                .comment(request.getComment())
                .isActive(true)
                .build();
        review = reviewRepository.save(review);

        // 4. Lưu hình ảnh
        List<ReviewImage> images = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                try {
                    String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                    Path path = Paths.get(UPLOAD_DIR + filename);
                    Files.write(path, file.getBytes());

                    ReviewImage image = ReviewImage.builder()
                            .reviewId(review.getId())
                            .imageUrl("/uploads/reviews/" + filename)
                            .build();
                    images.add(reviewImageRepository.save(image));
                } catch (IOException e) {
                    throw new RuntimeException("Lỗi khi lưu hình ảnh: " + e.getMessage());
                }
            }
        }
        review.setImages(images);

        return ReviewResponse.fromEntity(review);
    }

    @Override
    @Transactional
    public ReviewResponse updateReviewByUser(Integer userId, Integer reviewId, String newComment) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Bình luận/Đánh giá không tồn tại"));

        if (!review.getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa bình luận này");
        }

        review.setComment(newComment);
        return ReviewResponse.fromEntity(reviewRepository.save(review));
    }

    @Override
    @Transactional
    public void deleteReviewByUser(Integer userId, Integer reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Bình luận/Đánh giá không tồn tại"));

        if (!review.getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa bình luận này");
        }

        reviewRepository.delete(review);
    }

    @Override
    @Transactional
    public ReviewResponse replyToReview(Integer reviewId, String reply) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Bình luận/Đánh giá không tồn tại"));

        review.setReply(reply);
        return ReviewResponse.fromEntity(reviewRepository.save(review));
    }
}
