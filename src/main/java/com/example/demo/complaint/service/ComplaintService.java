package com.example.demo.complaint.service;

import com.example.demo.complaint.dto.ComplaintResponse;
import com.example.demo.complaint.dto.RefundCreateRequest;
import com.example.demo.complaint.dto.RefundResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ComplaintService {
    ComplaintResponse createComplaint(Integer orderId, String reason, String description, MultipartFile file);
    List<ComplaintResponse> getMyComplaints();
    RefundResponse createRefundRequest(Integer complaintId, RefundCreateRequest request);
    List<ComplaintResponse> getAllComplaints();
    ComplaintResponse updateComplaintStatus(Integer id, String status);
    RefundResponse updateRefundStatus(Integer refundId, String status);
}
