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

    private int getAvailableStock(Product product, String details) {
        int stock = product.getStock() != null ? product.getStock() : 0;
        String safeDetails = details != null ? details : "";
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            for (com.example.demo.product.entity.ProductVariant v : product.getVariants()) {
                if (safeDetails.contains(v.getSize() != null ? v.getSize() : "") &&
                    safeDetails.contains(v.getColor() != null ? v.getColor() : "") &&
                    safeDetails.contains(v.getWeight() != null ? v.getWeight() : "")) {
                    return v.getStock() != null ? v.getStock() : 0;
                }
            }
        }
        return stock;
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

        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            if (request.getDetails() == null || request.getDetails().trim().isEmpty()) {
                throw new RuntimeException("Vui lòng chọn thông số sản phẩm trước khi thêm vào giỏ hàng!");
            }
        }

        int availableStock = getAvailableStock(product, request.getDetails());

        if (existing.isPresent()) {
            CartItem cartItem = existing.get();
            if (cartItem.getQuantity() + request.getQuantity() > availableStock) {
                throw new RuntimeException("Số lượng yêu cầu vượt quá sản phẩm có sẵn trong kho!");
            }
            cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
            cartItemRepository.save(cartItem);
        } else {
            if (request.getQuantity() > availableStock) {
                throw new RuntimeException("Số lượng yêu cầu vượt quá sản phẩm có sẵn trong kho!");
            }
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

        int availableStock = getAvailableStock(cartItem.getProduct(), details);
        if (quantity > availableStock) {
            throw new RuntimeException("Số lượng yêu cầu vượt quá sản phẩm có sẵn trong kho!");
        }

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
