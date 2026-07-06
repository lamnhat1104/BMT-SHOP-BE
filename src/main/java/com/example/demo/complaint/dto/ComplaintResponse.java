package com.example.demo.complaint.dto;

import com.example.demo.complaint.entity.Complaint;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ComplaintResponse {
    private Integer id;
    private Integer orderId;
    private String orderCode;
    private Integer userId;
    private String userName;
    private String reason;
    private String description;
    private String evidenceUrl;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ComplaintResponse(Complaint entity) {
        this.id = entity.getId();
        if (entity.getOrder() != null) {
            this.orderId = entity.getOrder().getId();
            this.orderCode = entity.getOrder().getOrderCode();
        }
        if (entity.getUser() != null) {
            this.userId = entity.getUser().getUserId();
            this.userName = entity.getUser().getFullName();
        }
        this.reason = entity.getReason();
        this.description = entity.getDescription();
        this.evidenceUrl = entity.getEvidenceUrl();
        this.status = entity.getStatus();
        this.createdAt = entity.getCreatedAt();
        this.updatedAt = entity.getUpdatedAt();
    }
}
