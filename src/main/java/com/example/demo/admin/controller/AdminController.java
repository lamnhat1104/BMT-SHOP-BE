package com.example.demo.admin.controller;

import com.example.demo.account.entity.User;
import com.example.demo.account.repository.UserRepository;
import com.example.demo.admin.dto.DashboardStatsResponse;
import com.example.demo.order.dto.OrderResponse;
import com.example.demo.order.entity.Order;
import com.example.demo.order.repository.OrderRepository;
import com.example.demo.product.dto.ProductResponse;
import com.example.demo.product.entity.Product;
import com.example.demo.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Vui lòng đăng nhập để thực hiện thao tác này!"));
    }

    private void checkAdminAccess() {
        User user = getCurrentUser();
        if (user.getRole() != User.Role.admin) {
            throw new RuntimeException("Bạn không có quyền truy cập chức năng này!");
        }
    }

    // 1. Dashboard API
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardStats() {
        checkAdminAccess();

        List<Order> allOrders = orderRepository.findAll();
        List<Product> allProducts = productRepository.findAll();
        List<User> allUsers = userRepository.findAll();

        // Total revenue: Sum of totalAmount for orders NOT "Đã hủy"
        double totalRevenue = allOrders.stream()
                .filter(o -> !"Đã hủy".equals(o.getStatus()))
                .mapToDouble(Order::getTotalAmount)
                .sum();

        // Low stock products: stock <= 10
        List<ProductResponse> lowStock = allProducts.stream()
                .filter(p -> p.getStock() != null && p.getStock() <= 10)
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());

        // Recent orders: top 5 sorted by createdAt desc
        List<OrderResponse> recentOrders = allOrders.stream()
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .limit(5)
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());

        // Revenue by status
        Map<String, Double> revenueByStatus = allOrders.stream()
                .collect(Collectors.groupingBy(
                        Order::getStatus,
                        Collectors.summingDouble(Order::getTotalAmount)
                ));

        // Monthly revenue for the last 6 months
        Map<String, Double> rawMonthly = allOrders.stream()
                .filter(o -> !"Đã hủy".equals(o.getStatus()))
                .collect(Collectors.groupingBy(
                        o -> {
                            LocalDateTime dt = o.getCreatedAt();
                            return dt.format(DateTimeFormatter.ofPattern("MM/yyyy"));
                        },
                        Collectors.summingDouble(Order::getTotalAmount)
                ));

        // Create list of 6 months ending with current month
        List<DashboardStatsResponse.MonthlyRevenue> monthlyRevenue = new ArrayList<>();
        LocalDateTime temp = LocalDateTime.now().minusMonths(5);
        for (int i = 0; i < 6; i++) {
            String monthKey = temp.format(DateTimeFormatter.ofPattern("MM/yyyy"));
            double rev = rawMonthly.getOrDefault(monthKey, 0.0);
            monthlyRevenue.add(new DashboardStatsResponse.MonthlyRevenue(monthKey, rev));
            temp = temp.plusMonths(1);
        }

        DashboardStatsResponse stats = DashboardStatsResponse.builder()
                .totalRevenue(totalRevenue)
                .totalOrders((long) allOrders.size())
                .totalUsers((long) allUsers.size())
                .totalProducts((long) allProducts.size())
                .lowStockProducts(lowStock)
                .recentOrders(recentOrders)
                .revenueByStatus(revenueByStatus)
                .monthlyRevenue(monthlyRevenue)
                .build();

        return ResponseEntity.ok(stats);
    }

    // 2. User Management APIs
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        checkAdminAccess();
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        checkAdminAccess();
        User userToUpdate = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        // Avoid self-demotion
        User currentAdmin = getCurrentUser();
        if (currentAdmin.getUserId().equals(id)) {
            return ResponseEntity.badRequest().body("Bạn không thể tự thay đổi quyền hạn của chính mình!");
        }

        String roleStr = body.get("role");
        try {
            User.Role role = User.Role.valueOf(roleStr);
            userToUpdate.setRole(role);
            userToUpdate.setUpdatedAt(LocalDateTime.now());
            User saved = userRepository.save(userToUpdate);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Quyền hạn không hợp lệ!");
        }
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<?> updateUserStatus(@PathVariable Integer id, @RequestBody Map<String, Boolean> body) {
        checkAdminAccess();
        User userToUpdate = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        // Avoid self-deactivation
        User currentAdmin = getCurrentUser();
        if (currentAdmin.getUserId().equals(id)) {
            return ResponseEntity.badRequest().body("Bạn không thể tự khóa tài khoản của chính mình!");
        }

        Boolean isActive = body.get("isActive");
        if (isActive == null) {
            return ResponseEntity.badRequest().body("Trạng thái hoạt động không hợp lệ!");
        }

        userToUpdate.setIsActive(isActive);
        userToUpdate.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(userToUpdate);
        return ResponseEntity.ok(saved);
    }

    // 3. Product Management APIs
    @PostMapping("/products")
    public ResponseEntity<?> createProduct(@RequestBody Product product) {
        checkAdminAccess();
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Tên sản phẩm không được để trống!");
        }
        if (product.getPrice() == null || product.getPrice() < 0) {
            return ResponseEntity.badRequest().body("Giá sản phẩm không hợp lệ!");
        }
        if (product.getStock() == null || product.getStock() < 0) {
            return ResponseEntity.badRequest().body("Số lượng tồn kho không hợp lệ!");
        }

        product.setCreatedAt(LocalDateTime.now());
        Product saved = productRepository.save(product);
        return ResponseEntity.ok(ProductResponse.fromEntity(saved));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Integer id, @RequestBody Product productDetails) {
        checkAdminAccess();
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm!"));

        if (productDetails.getName() == null || productDetails.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Tên sản phẩm không được để trống!");
        }
        if (productDetails.getPrice() == null || productDetails.getPrice() < 0) {
            return ResponseEntity.badRequest().body("Giá sản phẩm không hợp lệ!");
        }
        if (productDetails.getStock() == null || productDetails.getStock() < 0) {
            return ResponseEntity.badRequest().body("Số lượng tồn kho không hợp lệ!");
        }

        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setStock(productDetails.getStock());
        product.setImageUrl(productDetails.getImageUrl());
        product.setBrand(productDetails.getBrand());
        product.setCategoryId(productDetails.getCategoryId());
        product.setCategoryName(productDetails.getCategoryName());

        Product saved = productRepository.save(product);
        return ResponseEntity.ok(ProductResponse.fromEntity(saved));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Integer id) {
        checkAdminAccess();
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm!"));

        productRepository.delete(product);
        return ResponseEntity.ok("Xóa sản phẩm thành công!");
    }

    @PutMapping("/products/{id}/stock")
    public ResponseEntity<?> updateProductStock(@PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        checkAdminAccess();
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm!"));

        Integer stock = body.get("stock");
        if (stock == null || stock < 0) {
            return ResponseEntity.badRequest().body("Số lượng tồn kho không hợp lệ!");
        }

        product.setStock(stock);
        Product saved = productRepository.save(product);
        return ResponseEntity.ok(ProductResponse.fromEntity(saved));
    }
}
