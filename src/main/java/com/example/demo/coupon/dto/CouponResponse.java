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
    private com.example.demo.coupon.entity.Coupon.DiscountType discountType;
    private Double discountValue;
    private Double minOrderValue;
    private Double maxDiscountAmount;
    private Integer maxUses;
    private Integer usedCount;
    private LocalDateTime expiredAt;
    private Boolean isActive;

    public static CouponResponse fromEntity(Coupon coupon) {
        if (coupon == null) {
            return null;
        }
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minOrderValue(coupon.getMinOrderValue())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .maxUses(coupon.getMaxUses())
                .usedCount(coupon.getUsedCount())
                .expiredAt(coupon.getExpiredAt())
                .isActive(coupon.getIsActive())
                .build();
    }
}
