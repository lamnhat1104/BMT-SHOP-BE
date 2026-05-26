package com.example.demo.cart.dto;

import lombok.Data;

@Data
public class CartItemRequest {
    private Integer productId;
    private Integer quantity;
    private String details;
}
