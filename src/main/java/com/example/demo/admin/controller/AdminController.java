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
import java.time.LocalDate;
import java.time.LocalTime;
import com.example.demo.order.entity.OrderDetail;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

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
                .mapToDouble(Order::getTotalPrice)
                .sum();

        // Low stock products: stock <= 10
        List<ProductResponse> lowStock = allProducts.stream()
                .filter(p -> p.getStock() != null && p.getStock() <= 10)
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());

        // Recent orders: top 5 sorted by createdAt desc
        List<OrderResponse> recentOrders = allOrders.stream()
                .sorted(Comparator.comparing(Order::getOrderDate).reversed())
                .limit(5)
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());

        // Revenue by status
        Map<String, Double> revenueByStatus = allOrders.stream()
                .collect(Collectors.groupingBy(
                        Order::getStatus,
                        Collectors.summingDouble(Order::getTotalPrice)
                ));

        // Orders by status
        Map<String, Long> ordersByStatus = allOrders.stream()
                .collect(Collectors.groupingBy(
                        Order::getStatus,
                        Collectors.counting()
                ));

        // Monthly revenue for the last 6 months
        Map<String, Double> rawMonthly = allOrders.stream()
                .filter(o -> !"Đã hủy".equals(o.getStatus()))
                .collect(Collectors.groupingBy(
                        o -> {
                            LocalDateTime dt = o.getOrderDate();
                            return dt.format(DateTimeFormatter.ofPattern("MM/yyyy"));
                        },
                        Collectors.summingDouble(Order::getTotalPrice)
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

        System.out.println("DEBUG rawMonthly: " + rawMonthly);
        System.out.println("DEBUG monthlyRevenue: " + monthlyRevenue);

        DashboardStatsResponse stats = DashboardStatsResponse.builder()
                .totalRevenue(totalRevenue)
                .totalOrders((long) allOrders.size())
                .totalUsers((long) allUsers.size())
                .totalProducts((long) allProducts.size())
                .lowStockProducts(lowStock)
                .recentOrders(recentOrders)
                .revenueByStatus(revenueByStatus)
                .ordersByStatus(ordersByStatus)
                .monthlyRevenue(monthlyRevenue)
                .build();

        return ResponseEntity.ok(stats);
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

    // 4. Report APIs
    @GetMapping("/reports/revenue")
    public ResponseEntity<?> getRevenueReport(
            @RequestParam(defaultValue = "7days") String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        checkAdminAccess();

        LocalDateTime start;
        LocalDateTime end = LocalDateTime.now();

        if ("7days".equals(period)) {
            start = LocalDate.now().minusDays(6).atStartOfDay();
        } else if ("30days".equals(period)) {
            start = LocalDate.now().minusDays(29).atStartOfDay();
        } else if ("month".equals(period)) {
            start = LocalDate.now().withDayOfYear(1).atStartOfDay();
        } else if ("year".equals(period)) {
            start = LocalDate.now().minusYears(4).withDayOfYear(1).atStartOfDay();
        } else if ("custom".equals(period) && startDate != null && endDate != null) {
            start = LocalDate.parse(startDate).atStartOfDay();
            end = LocalDate.parse(endDate).atTime(LocalTime.MAX);
        } else {
            return ResponseEntity.badRequest().body("Tham số thời gian không hợp lệ!");
        }

        final LocalDateTime finalStart = start;
        final LocalDateTime finalEnd = end;

        List<Order> orders = orderRepository.findAll().stream()
                .filter(o -> !o.getOrderDate().isBefore(finalStart) && !o.getOrderDate().isAfter(finalEnd))
                .filter(o -> !"Đã hủy".equals(o.getStatus()))
                .collect(Collectors.toList());

        List<RevenueReportPoint> result = new ArrayList<>();

        if ("7days".equals(period) || "30days".equals(period) || ("custom".equals(period) && java.time.temporal.ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate()) <= 31)) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
            Map<String, List<Order>> grouped = orders.stream()
                    .collect(Collectors.groupingBy(o -> o.getOrderDate().format(formatter)));

            LocalDate temp = start.toLocalDate();
            LocalDate last = end.toLocalDate();
            while (!temp.isAfter(last)) {
                String label = temp.format(formatter);
                List<Order> dayOrders = grouped.getOrDefault(label, new ArrayList<>());
                double rev = dayOrders.stream().mapToDouble(Order::getTotalPrice).sum();
                result.add(new RevenueReportPoint(label, rev, (long) dayOrders.size()));
                temp = temp.plusDays(1);
            }
        } else if ("month".equals(period) || ("custom".equals(period) && java.time.temporal.ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate()) <= 365)) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yyyy");
            Map<String, List<Order>> grouped = orders.stream()
                    .collect(Collectors.groupingBy(o -> o.getOrderDate().format(formatter)));

            LocalDate temp = start.toLocalDate().withDayOfMonth(1);
            LocalDate last = end.toLocalDate().withDayOfMonth(1);
            while (!temp.isAfter(last)) {
                String label = temp.format(formatter);
                List<Order> monthOrders = grouped.getOrDefault(label, new ArrayList<>());
                double rev = monthOrders.stream().mapToDouble(Order::getTotalPrice).sum();
                result.add(new RevenueReportPoint(label, rev, (long) monthOrders.size()));
                temp = temp.plusMonths(1);
            }
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy");
            Map<String, List<Order>> grouped = orders.stream()
                    .collect(Collectors.groupingBy(o -> o.getOrderDate().format(formatter)));

            LocalDate temp = start.toLocalDate().withDayOfYear(1);
            LocalDate last = end.toLocalDate().withDayOfYear(1);
            while (!temp.isAfter(last)) {
                String label = temp.format(formatter);
                List<Order> yearOrders = grouped.getOrDefault(label, new ArrayList<>());
                double rev = yearOrders.stream().mapToDouble(Order::getTotalPrice).sum();
                result.add(new RevenueReportPoint(label, rev, (long) yearOrders.size()));
                temp = temp.plusYears(1);
            }
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/reports/category-revenue")
    public ResponseEntity<?> getCategoryRevenue(
            @RequestParam(defaultValue = "7days") String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        checkAdminAccess();

        LocalDateTime start;
        LocalDateTime end = LocalDateTime.now();

        if ("7days".equals(period)) {
            start = LocalDate.now().minusDays(6).atStartOfDay();
        } else if ("30days".equals(period)) {
            start = LocalDate.now().minusDays(29).atStartOfDay();
        } else if ("month".equals(period)) {
            start = LocalDate.now().withDayOfYear(1).atStartOfDay();
        } else if ("year".equals(period)) {
            start = LocalDate.now().minusYears(4).withDayOfYear(1).atStartOfDay();
        } else if ("custom".equals(period) && startDate != null && endDate != null) {
            start = LocalDate.parse(startDate).atStartOfDay();
            end = LocalDate.parse(endDate).atTime(LocalTime.MAX);
        } else {
            return ResponseEntity.badRequest().body("Tham số thời gian không hợp lệ!");
        }

        final LocalDateTime finalStart = start;
        final LocalDateTime finalEnd = end;

        List<Order> orders = orderRepository.findAll().stream()
                .filter(o -> !o.getOrderDate().isBefore(finalStart) && !o.getOrderDate().isAfter(finalEnd))
                .filter(o -> !"Đã hủy".equals(o.getStatus()))
                .collect(Collectors.toList());

        Map<Integer, CategoryRevenuePoint> categoryStats = new HashMap<>();

        for (Order order : orders) {
            if (order.getOrderDetails() != null) {
                for (OrderDetail detail : order.getOrderDetails()) {
                    Product product = detail.getProduct();
                    if (product != null && product.getCategory() != null) {
                        Integer catId = product.getCategoryId();
                        String catName = product.getCategory().getName();
                        double detailRevenue = detail.getQuantity() * detail.getUnitPrice();
                        
                        CategoryRevenuePoint point = categoryStats.computeIfAbsent(catId, k -> CategoryRevenuePoint.builder()
                                .categoryId(catId)
                                .categoryName(catName)
                                .revenue(0.0)
                                .quantitySold(0L)
                                .build());
                        
                        point.setRevenue(point.getRevenue() + detailRevenue);
                        point.setQuantitySold(point.getQuantitySold() + detail.getQuantity());
                    }
                }
            }
        }

        return ResponseEntity.ok(new ArrayList<>(categoryStats.values()));
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RevenueReportPoint {
        private String label;
        private Double revenue;
        private Long orderCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryRevenuePoint {
        private Integer categoryId;
        private String categoryName;
        private Double revenue;
        private Long quantitySold;
    }
}
