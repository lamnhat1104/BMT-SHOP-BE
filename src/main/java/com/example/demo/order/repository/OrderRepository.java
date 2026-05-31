package com.example.demo.order.repository;

import com.example.demo.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    Optional<Order> findByOrderCode(String orderCode);
    List<Order> findByUserIdOrderByOrderDateDesc(Integer userId);
    List<Order> findAllByOrderByOrderDateDesc();
}
