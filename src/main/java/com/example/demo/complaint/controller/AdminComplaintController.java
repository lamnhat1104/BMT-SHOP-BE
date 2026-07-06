package com.example.demo.complaint.controller;

import com.example.demo.complaint.dto.ComplaintResponse;
import com.example.demo.complaint.dto.ComplaintStatusRequest;
import com.example.demo.complaint.dto.RefundResponse;
import com.example.demo.complaint.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/complaints")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasAuthority('admin')")
public class AdminComplaintController {

    private final ComplaintService complaintService;

    @GetMapping
    public ResponseEntity<List<ComplaintResponse>> getAllComplaints() {
        return ResponseEntity.ok(complaintService.getAllComplaints());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ComplaintResponse> updateComplaintStatus(
            @PathVariable Integer id,
            @RequestBody ComplaintStatusRequest request) {
        return ResponseEntity.ok(complaintService.updateComplaintStatus(id, request.getStatus()));
    }

    @PutMapping("/refunds/{id}/status")
    public ResponseEntity<RefundResponse> updateRefundStatus(
            @PathVariable Integer id,
            @RequestBody ComplaintStatusRequest request) {
        return ResponseEntity.ok(complaintService.updateRefundStatus(id, request.getStatus()));
    }
}
