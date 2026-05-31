package com.example.demo.admin.dto;

import com.example.demo.order.dto.OrderResponse;
import com.example.demo.product.dto.ProductResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private Double totalRevenue;
    private Long totalOrders;
    private Long totalUsers;
    private Long totalProducts;
    private List<ProductResponse> lowStockProducts;
    private List<OrderResponse> recentOrders;
    private Map<String, Double> revenueByStatus;
    private List<MonthlyRevenue> monthlyRevenue;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class MonthlyRevenue {
        private String month; // e.g. "05/2026"
        private Double revenue;
    }
}
