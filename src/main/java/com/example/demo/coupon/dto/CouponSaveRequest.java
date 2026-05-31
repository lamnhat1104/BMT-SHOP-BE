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

    @NotNull(message = "Mức giảm giá không được để trống")
    @Min(value = 1, message = "Mức giảm giá tối thiểu là 1%")
    @Max(value = 100, message = "Mức giảm giá tối đa là 100%")
    private Integer discountPercent;

    @NotNull(message = "Ngày hết hạn không được để trống")
    private LocalDateTime expiredAt;

    private Boolean isActive;
}
