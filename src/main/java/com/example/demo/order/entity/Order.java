package com.example.demo.order.entity;

import com.example.demo.account.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Integer id;

    @Column(name = "order_code", unique = true, nullable = false)
    private String orderCode;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "receiver_name", nullable = false)
    private String fullName;

    @Column(name = "receiver_phone", nullable = false)
    private String phone;

    @Column(name = "shipping_address", nullable = false)
    private String address;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod; // e.g., "COD", "BANK_TRANSFER", "WALLET"

    @Column(nullable = false)
    private String status; // "Chờ xác nhận", "Đang xử lý", "Đang giao hàng", "Hoàn thành", "Đã hủy"

    @Column(name = "total_price", nullable = false)
    private Double totalAmount;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime createdAt;

    @Transient
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items;
}
