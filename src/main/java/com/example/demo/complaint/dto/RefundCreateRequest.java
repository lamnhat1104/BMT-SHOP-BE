package com.example.demo.complaint.dto;

import lombok.Data;

@Data
public class RefundCreateRequest {
    private Double amount;
    private String method; // bank, wallet, original
    private String reason;
}
