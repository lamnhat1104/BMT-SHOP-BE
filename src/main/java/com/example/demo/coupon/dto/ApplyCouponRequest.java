package com.example.demo.coupon.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

@Data
public class ApplyCouponRequest {
    @NotBlank(message = "Mã khuyến mãi không được để trống")
    private String code;

    @NotNull(message = "Tổng tiền giỏ hàng không được để trống")
    @Min(value = 0)
    private Double cartTotal;
}
