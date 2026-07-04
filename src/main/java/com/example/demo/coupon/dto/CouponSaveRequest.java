package com.example.demo.coupon.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponSaveRequest {

    @NotBlank(message = "Mã khuyến mãi không được để trống")
    @Size(max = 50, message = "Mã khuyến mãi không được quá 50 ký tự")
    private String code;

    @NotNull(message = "Loại giảm giá không được để trống")
    private com.example.demo.coupon.entity.Coupon.DiscountType discountType;

    @NotNull(message = "Giá trị giảm giá không được để trống")
    @Min(value = 0, message = "Giá trị giảm giá phải lớn hơn hoặc bằng 0")
    private Double discountValue;

    @Min(value = 0, message = "Giá trị đơn hàng tối thiểu phải lớn hơn hoặc bằng 0")
    private Double minOrderValue;

    @Min(value = 0, message = "Số tiền giảm tối đa phải lớn hơn hoặc bằng 0")
    private Double maxDiscountAmount;

    @Min(value = 1, message = "Số lần sử dụng tối đa phải lớn hơn hoặc bằng 1")
    private Integer maxUses;

    @NotNull(message = "Ngày hết hạn không được để trống")
    private LocalDateTime expiredAt;

    private Boolean isActive;
}
