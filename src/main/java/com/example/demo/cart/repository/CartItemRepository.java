package com.example.demo.cart.repository;

import com.example.demo.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    List<CartItem> findByUserUserId(Integer userId);
    Optional<CartItem> findByUserUserIdAndProductIdAndDetails(Integer userId, Integer productId, String details);
    void deleteByUserUserId(Integer userId);
}
