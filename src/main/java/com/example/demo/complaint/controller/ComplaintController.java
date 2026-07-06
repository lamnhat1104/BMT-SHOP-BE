package com.example.demo.complaint.controller;

import com.example.demo.complaint.dto.ComplaintResponse;
import com.example.demo.complaint.dto.RefundCreateRequest;
import com.example.demo.complaint.dto.RefundResponse;
import com.example.demo.complaint.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ComplaintController {

    private final ComplaintService complaintService;

    @PostMapping
    public ResponseEntity<ComplaintResponse> createComplaint(
            @RequestParam("orderId") Integer orderId,
            @RequestParam("reason") String reason,
            @RequestParam("description") String description,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        
        return ResponseEntity.ok(complaintService.createComplaint(orderId, reason, description, file));
    }

    @GetMapping
    public ResponseEntity<List<ComplaintResponse>> getMyComplaints() {
        return ResponseEntity.ok(complaintService.getMyComplaints());
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<RefundResponse> createRefundRequest(
            @PathVariable Integer id,
            @RequestBody RefundCreateRequest request) {
        return ResponseEntity.ok(complaintService.createRefundRequest(id, request));
    }
}
