package com.example.demo.coupon.dto;

import com.example.demo.coupon.entity.Coupon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponResponse {
    private Integer id;
    private String code;
    private Integer discountPercent;
    private LocalDateTime expiredAt;
    private Boolean isActive;

    public static CouponResponse fromEntity(Coupon coupon) {
        if (coupon == null) {
            return null;
        }
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .discountPercent(coupon.getDiscountPercent())
                .expiredAt(coupon.getExpiredAt())
                .isActive(coupon.getIsActive())
                .build();
    }
}
