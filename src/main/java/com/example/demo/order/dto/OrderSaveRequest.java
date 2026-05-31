package com.example.demo.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSaveRequest {
    @NotBlank(message = "Họ tên người nhận không được để trống")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;

    @NotBlank(message = "Địa chỉ nhận hàng không được để trống")
    private String address;

    private String notes;

    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod;
}
