package com.example.demo.complaint.service;

import com.example.demo.account.entity.User;
import com.example.demo.account.repository.UserRepository;
import com.example.demo.complaint.dto.ComplaintResponse;
import com.example.demo.complaint.dto.RefundCreateRequest;
import com.example.demo.complaint.dto.RefundResponse;
import com.example.demo.complaint.entity.Complaint;
import com.example.demo.complaint.entity.RefundRequest;
import com.example.demo.complaint.repository.ComplaintRepository;
import com.example.demo.complaint.repository.RefundRequestRepository;
import com.example.demo.config.CloudinaryService;
import com.example.demo.order.entity.Order;
import com.example.demo.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final CloudinaryService cloudinaryService;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin tài khoản"));
    }

    @Override
    public ComplaintResponse createComplaint(Integer orderId, String reason, String description, MultipartFile file) {
        User user = getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (!order.getUserId().equals(user.getUserId())) {
            throw new RuntimeException("Bạn không có quyền khiếu nại đơn hàng này");
        }

        String evidenceUrl = null;
        if (file != null && !file.isEmpty()) {
            try {
                evidenceUrl = cloudinaryService.uploadFile(file);
            } catch (Exception e) {
                throw new RuntimeException("Lỗi khi tải lên minh chứng: " + e.getMessage());
            }
        }

        Complaint complaint = Complaint.builder()
                .user(user)
                .order(order)
                .reason(reason)
                .description(description)
                .evidenceUrl(evidenceUrl)
                .status("pending")
                .build();

        Complaint saved = complaintRepository.save(complaint);
        return new ComplaintResponse(saved);
    }

    @Override
    public List<ComplaintResponse> getMyComplaints() {
        User user = getCurrentUser();
        return complaintRepository.findByUser_UserIdOrderByCreatedAtDesc(user.getUserId())
                .stream()
                .map(ComplaintResponse::new)
                .collect(Collectors.toList());
    }

    @Override
    public RefundResponse createRefundRequest(Integer complaintId, RefundCreateRequest request) {
        User user = getCurrentUser();
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khiếu nại"));

        if (!complaint.getUser().getUserId().equals(user.getUserId())) {
            throw new RuntimeException("Bạn không có quyền yêu cầu hoàn tiền cho khiếu nại này");
        }
        
        if (!"approved".equals(complaint.getStatus())) {
            throw new RuntimeException("Khiếu nại phải được chấp thuận (approved) mới có thể yêu cầu hoàn tiền");
        }

        if (refundRequestRepository.findByComplaint_Id(complaintId).isPresent()) {
            throw new RuntimeException("Khiếu nại này đã có yêu cầu hoàn tiền");
        }

        RefundRequest refundRequest = RefundRequest.builder()
                .complaint(complaint)
                .amount(request.getAmount())
                .method(request.getMethod())
                .reason(request.getReason())
                .status("pending")
                .build();

        RefundRequest saved = refundRequestRepository.save(refundRequest);
        return new RefundResponse(saved);
    }

    @Override
    public List<ComplaintResponse> getAllComplaints() {
        return complaintRepository.findAll()
                .stream()
                .map(ComplaintResponse::new)
                .collect(Collectors.toList());
    }

    @Override
    public ComplaintResponse updateComplaintStatus(Integer id, String status) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khiếu nại"));
        complaint.setStatus(status);
        Complaint saved = complaintRepository.save(complaint);
        return new ComplaintResponse(saved);
    }

    @Override
    public RefundResponse updateRefundStatus(Integer refundId, String status) {
        RefundRequest refundRequest = refundRequestRepository.findById(refundId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu hoàn tiền"));
        refundRequest.setStatus(status);
        RefundRequest saved = refundRequestRepository.save(refundRequest);
        
        // Cập nhật trạng thái của Complaint tương ứng nếu refund được approve
        if ("processed".equals(status)) {
            Complaint complaint = refundRequest.getComplaint();
            complaint.setStatus("refunded");
            complaintRepository.save(complaint);
        }
        
        return new RefundResponse(saved);
    }
}
