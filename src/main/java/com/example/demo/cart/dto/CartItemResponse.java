package com.example.demo.cart.dto;

import com.example.demo.cart.entity.CartItem;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CartItemResponse {
    private Integer id; // Khớp với product.id ở Frontend
    private String name;
    private Double price;
    private String thumbnail; // Ánh xạ từ product.imageUrl
    private String brand;
    private Integer quantity;
    private String details;

    public static CartItemResponse fromEntity(CartItem cartItem) {
        String name = cartItem.getProduct().getName();
        String brand = "Unknown Brand";
        if (name != null) {
            String upper = name.toUpperCase();
            if (upper.contains("YONEX")) brand = "YONEX";
            else if (upper.contains("LINING")) brand = "LINING";
            else if (upper.contains("VICTOR")) brand = "VICTOR";
            else if (upper.contains("MIZUNO")) brand = "MIZUNO";
        }
        
        return CartItemResponse.builder()
                .id(cartItem.getProduct().getId())
                .name(name)
                .price(cartItem.getProduct().getPrice())
                .thumbnail(cartItem.getProduct().getImageUrl())
                .brand(brand)
                .quantity(cartItem.getQuantity())
                .details(cartItem.getDetails())
                .build();
    }
}
