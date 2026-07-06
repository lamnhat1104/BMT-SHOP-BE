package com.example.demo.cart.dto;

import com.example.demo.cart.entity.CartItem;
import com.example.demo.product.entity.ProductVariant;
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
    private Integer stock;

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
        
        int stock = cartItem.getProduct().getStock() != null ? cartItem.getProduct().getStock() : 0;
        String details = cartItem.getDetails() != null ? cartItem.getDetails() : "";
        if (cartItem.getProduct().getVariants() != null && !cartItem.getProduct().getVariants().isEmpty()) {
            for (ProductVariant v : cartItem.getProduct().getVariants()) {
                if (details.contains(v.getSize() != null ? v.getSize() : "") &&
                    details.contains(v.getColor() != null ? v.getColor() : "") &&
                    details.contains(v.getWeight() != null ? v.getWeight() : "")) {
                    stock = v.getStock() != null ? v.getStock() : 0;
                    break;
                }
            }
        }
        
        return CartItemResponse.builder()
                .id(cartItem.getProduct().getId())
                .name(name)
                .price(cartItem.getProduct().getPrice())
                .thumbnail(cartItem.getProduct().getImageUrl())
                .brand(brand)
                .quantity(cartItem.getQuantity())
                .details(cartItem.getDetails())
                .stock(stock)
                .build();
    }
}
