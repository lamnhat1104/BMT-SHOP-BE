package com.example.demo.order.service;

import com.example.demo.account.entity.User;
import com.example.demo.account.repository.UserRepository;
import com.example.demo.cart.entity.CartItem;
import com.example.demo.cart.repository.CartItemRepository;
import com.example.demo.order.dto.OrderRequest;
import com.example.demo.order.dto.OrderResponse;
import com.example.demo.order.dto.OrderStatusRequest;
import com.example.demo.order.entity.Order;
import com.example.demo.order.entity.OrderItem;
import com.example.demo.order.repository.OrderItemRepository;
import com.example.demo.order.repository.OrderRepository;
import com.example.demo.product.entity.Product;
import com.example.demo.product.repository.ProductRepository;
import com.example.demo.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final EmailService emailService;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Vui lòng đăng nhập để thực hiện thao tác này!"));
    }

    private void checkAdminAccess() {
        User user = getCurrentUser();
        if (user.getRole() != User.Role.admin) {
            throw new RuntimeException("Bạn không có quyền thực hiện thao tác này!");
        }
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        User user = getCurrentUser();

        // 1. Get user cart items
        List<CartItem> cartItems = cartItemRepository.findByUserUserId(user.getUserId());
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Giỏ hàng của bạn đang trống! Vui lòng thêm sản phẩm trước khi thanh toán.");
        }

        // 2. Validate inputs
        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new RuntimeException("Họ tên người nhận không được để trống!");
        }
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            throw new RuntimeException("Số điện thoại không được để trống!");
        }
        if (request.getAddress() == null || request.getAddress().trim().isEmpty()) {
            throw new RuntimeException("Địa chỉ nhận hàng không được để trống!");
        }

        // 3. Check stock & subtract
        double totalAmount = 0.0;
        for (CartItem item : cartItems) {
            Product product = item.getProduct();
            if (product.getStock() < item.getQuantity()) {
                throw new RuntimeException("Sản phẩm '" + product.getName() + "' không đủ số lượng tồn kho! (Tồn kho còn: " + product.getStock() + ")");
            }
            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);
            totalAmount += product.getPrice() * item.getQuantity();
        }

        // 4. Generate professional order code
        String orderCode = "BMT" + System.currentTimeMillis() + String.format("%03d", (int)(Math.random() * 1000));

        // 5. Create Order
        Order order = Order.builder()
                .orderCode(orderCode)
                .user(user)
                .fullName(request.getFullName().trim())
                .phone(request.getPhone().trim())
                .address(request.getAddress().trim())
                .notes(request.getNotes() != null ? request.getNotes().trim() : null)
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "COD")
                .status("Chờ xác nhận")
                .totalAmount(totalAmount)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Order savedOrder = orderRepository.save(order);

        // 6. Create Order Items
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem item : cartItems) {
            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .product(item.getProduct())
                    .quantity(item.getQuantity())
                    .price(item.getProduct().getPrice())
                    .details(item.getDetails())
                    .build();
            orderItems.add(orderItemRepository.save(orderItem));
        }

        savedOrder.setItems(orderItems);

        // 7. Clear the user's cart
        cartItemRepository.deleteByUserUserId(user.getUserId());

        // 8. Send Order Confirmation Email
        emailService.sendOrderConfirmationEmail(user.getEmail(), orderCode, totalAmount, request.getFullName());

        return OrderResponse.fromEntity(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForCurrentUser() {
        User user = getCurrentUser();
        return orderRepository.findByUserUserIdOrderByCreatedAtDesc(user.getUserId()).stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        checkAdminAccess();
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateOrderStatus(Integer id, OrderStatusRequest request) {
        checkAdminAccess();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));

        String oldStatus = order.getStatus();
        String newStatus = request.getStatus();

        if (oldStatus.equals(newStatus)) {
            return OrderResponse.fromEntity(order);
        }

        // Handle inventory when cancelled by admin
        if (newStatus.equals("Đã hủy") && !oldStatus.equals("Đã hủy")) {
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
            }
        }
        // Handle inventory if recovered from cancelled status
        else if (oldStatus.equals("Đã hủy") && !newStatus.equals("Đã hủy")) {
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                if (product.getStock() < item.getQuantity()) {
                    throw new RuntimeException("Không thể mở lại đơn vì sản phẩm '" + product.getName() + "' đã hết hàng tồn kho!");
                }
                product.setStock(product.getStock() - item.getQuantity());
                productRepository.save(product);
            }
        }

        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        Order updated = orderRepository.save(order);
        return OrderResponse.fromEntity(updated);
    }

    @Transactional
    public OrderResponse cancelOrder(Integer id) {
        User user = getCurrentUser();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));

        // Check ownership
        if (!order.getUser().getUserId().equals(user.getUserId())) {
            throw new RuntimeException("Bạn không có quyền hủy đơn hàng này!");
        }

        // Check if cancellable (only "Chờ xác nhận" can be cancelled by user)
        if (!order.getStatus().equals("Chờ xác nhận")) {
            throw new RuntimeException("Đơn hàng đang được xử lý hoặc đã giao, không thể tự hủy. Vui lòng liên hệ Hotline!");
        }

        order.setStatus("Đã hủy");
        order.setUpdatedAt(LocalDateTime.now());
        Order updated = orderRepository.save(order);

        // Restore stock
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }

        return OrderResponse.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public OrderResponse trackOrder(String orderCode, String phone) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Mã đơn hàng không chính xác hoặc không tồn tại!"));

        if (!order.getPhone().equals(phone.trim())) {
            throw new RuntimeException("Số điện thoại đặt hàng không khớp với đơn hàng!");
        }

        return OrderResponse.fromEntity(order);
    }
}
