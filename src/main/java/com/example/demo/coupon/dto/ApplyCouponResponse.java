package com.example.demo.coupon.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApplyCouponResponse {
    private Boolean isValid;
    private Double discountAmount;
    private String message;
    private CouponResponse coupon;
}
