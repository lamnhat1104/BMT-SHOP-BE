package com.example.demo.cart.controller;

import com.example.demo.cart.dto.CartItemRequest;
import com.example.demo.cart.dto.CartItemResponse;
import com.example.demo.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping
    public ResponseEntity<List<CartItemResponse>> getCart() {
        return ResponseEntity.ok(cartService.getCart());
    }

    @PostMapping
    public ResponseEntity<List<CartItemResponse>> addCartItem(@RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartService.addCartItem(request));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<List<CartItemResponse>> updateCartItemQuantity(
            @PathVariable Integer productId,
            @RequestParam Integer quantity,
            @RequestParam String details) {
        return ResponseEntity.ok(cartService.updateCartItemQuantity(productId, quantity, details));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<List<CartItemResponse>> removeCartItem(
            @PathVariable Integer productId,
            @RequestParam String details) {
        return ResponseEntity.ok(cartService.removeCartItem(productId, details));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart() {
        cartService.clearCart();
        return ResponseEntity.ok().build();
    }
}
