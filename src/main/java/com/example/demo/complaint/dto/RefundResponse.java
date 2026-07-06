package com.example.demo.complaint.dto;

import com.example.demo.complaint.entity.RefundRequest;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RefundResponse {
    private Integer id;
    private Integer complaintId;
    private Double amount;
    private String method;
    private String reason;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RefundResponse(RefundRequest entity) {
        this.id = entity.getId();
        this.complaintId = entity.getComplaint().getId();
        this.amount = entity.getAmount();
        this.method = entity.getMethod();
        this.reason = entity.getReason();
        this.status = entity.getStatus();
        this.createdAt = entity.getCreatedAt();
        this.updatedAt = entity.getUpdatedAt();
    }
}
