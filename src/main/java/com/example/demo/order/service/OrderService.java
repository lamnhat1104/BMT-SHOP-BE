package com.example.demo.order.service;

import com.example.demo.order.dto.OrderResponse;
import com.example.demo.order.dto.OrderSaveRequest;
import java.util.List;

public interface OrderService {
    OrderResponse createOrder(OrderSaveRequest request);
    List<OrderResponse> getMyOrders();
    List<OrderResponse> getAdminOrders();
    OrderResponse updateOrderStatus(Integer orderId, String status);
    OrderResponse cancelOrder(Integer orderId);
    OrderResponse trackOrder(String orderCode, String phone);
}
