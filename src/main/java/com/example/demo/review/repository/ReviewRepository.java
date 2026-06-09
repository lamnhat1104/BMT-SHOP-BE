package com.example.demo.review.repository;

import com.example.demo.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {
    List<Review> findByProductIdAndIsActiveTrueOrderByCreatedAtDesc(Integer productId);
    boolean existsByOrderIdAndProductId(Integer orderId, Integer productId);
}
