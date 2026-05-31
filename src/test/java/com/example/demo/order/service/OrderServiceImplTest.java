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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderDetailRepository orderDetailRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User sampleUser;
    private Product sampleProduct;
    private CartItem sampleCartItem;
    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("buyer@gmail.com");

        sampleUser = new User();
        sampleUser.setUserId(1);
        sampleUser.setEmail("buyer@gmail.com");
        sampleUser.setFullName("Nguyen Van A");

        sampleProduct = Product.builder()
                .id(10)
                .name("Astrox 99")
                .price(3000000.0)
                .stock(5)
                .status("available")
                .build();

        sampleCartItem = CartItem.builder()
                .id(100)
                .user(sampleUser)
                .product(sampleProduct)
                .quantity(2)
                .details("3U/G5")
                .build();

        sampleOrder = Order.builder()
                .id(50)
                .orderCode("BMT999999")
                .userId(1)
                .totalPrice(6000000.0)
                .status("Chờ xác nhận")
                .receiverName("Nguyen Van A")
                .receiverPhone("0987654321")
                .shippingAddress("Hanoi")
                .build();
    }

    @Test
    void createOrder_Success_ShouldClearCartAndReduceStock() {
        when(userRepository.findByEmail("buyer@gmail.com")).thenReturn(Optional.of(sampleUser));
        when(cartItemRepository.findByUserUserId(1)).thenReturn(List.of(sampleCartItem));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(50);
            return o;
        });
        when(orderDetailRepository.save(any(OrderDetail.class))).thenAnswer(inv -> {
            OrderDetail od = inv.getArgument(0);
            od.setId(500);
            return od;
        });

        OrderSaveRequest request = OrderSaveRequest.builder()
                .fullName("Nguyen Van A")
                .phone("0987654321")
                .address("Hanoi")
                .paymentMethod("COD")
                .build();

        OrderResponse response = orderService.createOrder(request);

        assertNotNull(response);
        assertEquals(50, response.getId());
        assertEquals("Nguyen Van A", response.getFullName());
        assertEquals(6000000.0, response.getTotalAmount());
        assertEquals(3, sampleProduct.getStock()); // Stock reduced from 5 to 3

        verify(productRepository, times(1)).save(sampleProduct);
        verify(cartItemRepository, times(1)).deleteByUserUserId(1);
    }

    @Test
    void createOrder_OutOfStock_ShouldThrowException() {
        sampleCartItem.setQuantity(10); // requested 10, stock is 5
        when(userRepository.findByEmail("buyer@gmail.com")).thenReturn(Optional.of(sampleUser));
        when(cartItemRepository.findByUserUserId(1)).thenReturn(List.of(sampleCartItem));

        OrderSaveRequest request = OrderSaveRequest.builder()
                .fullName("Nguyen Van A")
                .phone("0987654321")
                .address("Hanoi")
                .paymentMethod("COD")
                .build();

        assertThrows(RuntimeException.class, () -> orderService.createOrder(request));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrderStatus_Cancelled_ShouldRefundStock() {
        OrderDetail detail = OrderDetail.builder()
                .productId(10)
                .product(sampleProduct)
                .quantity(2)
                .build();
        sampleOrder.setOrderDetails(List.of(detail));

        when(orderRepository.findById(50)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);

        OrderResponse response = orderService.updateOrderStatus(50, "Đã hủy");

        assertNotNull(response);
        assertEquals("Đã hủy", response.getStatus());
        assertEquals(7, sampleProduct.getStock()); // Refunded: 5 + 2 = 7

        verify(productRepository, times(1)).save(sampleProduct);
    }
}
