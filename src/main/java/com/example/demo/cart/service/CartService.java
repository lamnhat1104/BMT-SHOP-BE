package com.example.demo.cart.service;

import com.example.demo.account.entity.User;
import com.example.demo.account.repository.UserRepository;
import com.example.demo.cart.dto.CartItemRequest;
import com.example.demo.cart.dto.CartItemResponse;
import com.example.demo.cart.entity.CartItem;
import com.example.demo.cart.repository.CartItemRepository;
import com.example.demo.product.entity.Product;
import com.example.demo.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Vui lòng đăng nhập để thực hiện thao tác này!"));
    }

    @Transactional(readOnly = true)
    public List<CartItemResponse> getCart() {
        User user = getCurrentUser();
        return cartItemRepository.findByUserUserId(user.getUserId()).stream()
                .map(CartItemResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<CartItemResponse> addCartItem(CartItemRequest request) {
        User user = getCurrentUser();
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại!"));

        Optional<CartItem> existing = cartItemRepository.findByUserUserIdAndProductIdAndDetails(
                user.getUserId(), request.getProductId(), request.getDetails());

        if (existing.isPresent()) {
            CartItem cartItem = existing.get();
            cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
            cartItemRepository.save(cartItem);
        } else {
            CartItem cartItem = CartItem.builder()
                    .user(user)
                    .product(product)
                    .quantity(request.getQuantity())
                    .details(request.getDetails())
                    .build();
            cartItemRepository.save(cartItem);
        }
        return getCart();
    }

    @Transactional
    public List<CartItemResponse> updateCartItemQuantity(Integer productId, Integer quantity, String details) {
        User user = getCurrentUser();
        CartItem cartItem = cartItemRepository.findByUserUserIdAndProductIdAndDetails(user.getUserId(), productId, details)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ hàng!"));

        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);
        return getCart();
    }

    @Transactional
    public List<CartItemResponse> removeCartItem(Integer productId, String details) {
        User user = getCurrentUser();
        CartItem cartItem = cartItemRepository.findByUserUserIdAndProductIdAndDetails(user.getUserId(), productId, details)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ hàng!"));

        cartItemRepository.delete(cartItem);
        return getCart();
    }

    @Transactional
    public void clearCart() {
        User user = getCurrentUser();
        cartItemRepository.deleteByUserUserId(user.getUserId());
    }
}
