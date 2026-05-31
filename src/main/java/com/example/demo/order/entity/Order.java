package com.example.demo.order.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Integer id;

    @Column(name = "order_code", nullable = false, unique = true, length = 50)
    private String orderCode;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "total_price")
    private Double totalPrice;

    @Column(name = "status")
    private String status; // 'pending','confirmed','shipping','completed','cancelled'

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "payment_status", columnDefinition = "ENUM('pending', 'paid', 'failed')")
    private String paymentStatus; // 'pending','paid','failed'

    @Column(name = "receiver_name", length = 100)
    private String receiverName;

    @Column(name = "receiver_phone", length = 20)
    private String receiverPhone;

    @Column(name = "shipping_address", length = 255)
    private String shippingAddress;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "order_date", nullable = false, updatable = false)
    private LocalDateTime orderDate;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderDetail> orderDetails;

    @PrePersist
    protected void onCreate() {
        this.orderDate = LocalDateTime.now();
        if (this.status == null) this.status = "pending";
        if (this.paymentStatus == null) this.paymentStatus = "pending";
    }
}
