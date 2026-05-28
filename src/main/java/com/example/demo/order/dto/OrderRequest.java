package com.example.demo.order.dto;

import lombok.Data;

@Data
public class OrderRequest {
    private String fullName;
    private String phone;
    private String address;
    private String notes;
    private String paymentMethod;
}
