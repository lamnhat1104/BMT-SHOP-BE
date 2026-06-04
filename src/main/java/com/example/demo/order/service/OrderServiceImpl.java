package com.example.demo.order.service;

import com.example.demo.account.entity.User;
import com.example.demo.account.repository.UserRepository;
import com.example.demo.cart.entity.CartItem;
import com.example.demo.cart.repository.CartItemRepository;
import com.example.demo.order.dto.OrderResponse;
import com.example.demo.order.dto.OrderSaveRequest;
import com.example.demo.order.entity.Order;
import com.example.demo.order.entity.OrderDetail;
import com.example.demo.order.repository.OrderDetailRepository;
import com.example.demo.order.repository.OrderRepository;
import com.example.demo.product.entity.Product;
import com.example.demo.product.repository.ProductRepository;
import com.example.demo.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.example.demo.payment.VNPAYConfig;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Vui lòng đăng nhập để thực hiện thao tác này!"));
    }

    @Override
    @Transactional
    public OrderResponse createOrder(OrderSaveRequest request) {
        User user = getCurrentUser();
        List<CartItem> cartItems = cartItemRepository.findByUserUserId(user.getUserId());
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Giỏ hàng của bạn đang trống, không thể đặt hàng!");
        }

        // Validate stock for all items first
        for (CartItem item : cartItems) {
            Product product = item.getProduct();
            if (product.getStock() < item.getQuantity()) {
                throw new RuntimeException("Sản phẩm '" + product.getName() + "' không đủ số lượng trong kho!");
            }
        }

        // Calculate total price
        double totalPrice = 0.0;
        for (CartItem item : cartItems) {
            Product product = item.getProduct();
            totalPrice += product.getPrice() * item.getQuantity();
        }

        // Generate a unique order code, e.g. BMT123456
        String orderCode = generateOrderCode();

        boolean isVNPay = "VNPAY".equalsIgnoreCase(request.getPaymentMethod());

        // Create order
        Order order = Order.builder()
                .orderCode(orderCode)
                .userId(user.getUserId())
                .totalPrice(totalPrice)
                .status(isVNPay ? "Chờ thanh toán" : "Chờ xác nhận") // Match frontend status badge display
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus("pending")
                .receiverName(request.getFullName())
                .receiverPhone(request.getPhone())
                .shippingAddress(request.getAddress())
                .notes(request.getNotes())
                .build();

        Order savedOrder = orderRepository.save(order);

        List<OrderDetail> savedDetails = new ArrayList<>();
        // Process order details and reduce stock
        for (CartItem item : cartItems) {
            Product product = item.getProduct();

            // Reduce stock
            product.setStock(product.getStock() - item.getQuantity());
            if (product.getStock() == 0) {
                product.setStatus("out_of_stock");
            }
            productRepository.save(product);

            // Create order detail
            OrderDetail detail = OrderDetail.builder()
                    .orderId(savedOrder.getId())
                    .productId(product.getId())
                    .product(product)
                    .quantity(item.getQuantity())
                    .unitPrice(product.getPrice())
                    .details(item.getDetails())
                    .build();
            
            savedDetails.add(orderDetailRepository.save(detail));
        }

        // Set order details list to avoid lazy loading issues on return
        savedOrder.setOrderDetails(savedDetails);

        // Clear user's cart
        cartItemRepository.deleteByUserUserId(user.getUserId());

        // Send Order Confirmation Email (Only immediately for COD, VNPay sends on success callback)
        if (!isVNPay) {
            try {
                emailService.sendOrderConfirmationEmail(user.getEmail(), orderCode, totalPrice, request.getFullName());
            } catch (Exception e) {
                System.err.println("OrderServiceImpl - Lỗi gửi mail xác nhận đơn hàng: " + e.getMessage());
            }
        }

        OrderResponse response = OrderResponse.fromEntity(savedOrder);
        
        if (isVNPay) {
            String ipAddress = "127.0.0.1";
            try {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    ipAddress = VNPAYConfig.getIpAddress(attributes.getRequest());
                }
            } catch (Exception e) {
                // Ignore fallback for testing
            }
            String paymentUrl = VNPAYConfig.createPaymentUrl(orderCode, totalPrice, ipAddress);
            response.setPaymentUrl(paymentUrl);
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders() {
        User user = getCurrentUser();
        return orderRepository.findByUserIdOrderByOrderDateDesc(user.getUserId()).stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAdminOrders() {
        return orderRepository.findAllByOrderByOrderDateDesc().stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Integer orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));
        
        order.setStatus(status);
        if ("Hoàn thành".equals(status)) {
            order.setPaymentStatus("paid");
        } else if ("Đã hủy".equals(status)) {
            order.setPaymentStatus("failed");
            // Refund stock if order is cancelled
            if (order.getOrderDetails() != null) {
                for (OrderDetail detail : order.getOrderDetails()) {
                    Product product = detail.getProduct();
                    if (product != null) {
                        product.setStock(product.getStock() + detail.getQuantity());
                        product.setStatus("available");
                        productRepository.save(product);
                    }
                }
            }
        }
        
        Order saved = orderRepository.save(order);
        return OrderResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Integer orderId) {
        User user = getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        if (!order.getUserId().equals(user.getUserId())) {
            throw new RuntimeException("Bạn không có quyền hủy đơn hàng này!");
        }

        if (!"Chờ xác nhận".equals(order.getStatus())) {
            throw new RuntimeException("Chỉ có thể hủy đơn hàng ở trạng thái 'Chờ xác nhận'!");
        }

        order.setStatus("Đã hủy");
        order.setPaymentStatus("failed");

        // Refund stock
        if (order.getOrderDetails() != null) {
            for (OrderDetail detail : order.getOrderDetails()) {
                Product product = detail.getProduct();
                if (product != null) {
                    product.setStock(product.getStock() + detail.getQuantity());
                    product.setStatus("available");
                    productRepository.save(product);
                }
            }
        }

        Order saved = orderRepository.save(order);
        return OrderResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse trackOrder(String orderCode, String phone) {
        Order order = orderRepository.findByOrderCode(orderCode.trim().toUpperCase())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với mã " + orderCode));

        if (!order.getReceiverPhone().equals(phone.trim())) {
            throw new RuntimeException("Số điện thoại nhận hàng không khớp với đơn hàng!");
        }

        return OrderResponse.fromEntity(order);
    }

    private String generateOrderCode() {
        Random random = new Random();
        int number = 100000 + random.nextInt(900000);
        return "BMT" + number;
    }
}
