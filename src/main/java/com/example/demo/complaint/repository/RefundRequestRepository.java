package com.example.demo.complaint.repository;

import com.example.demo.complaint.entity.RefundRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, Integer> {
    Optional<RefundRequest> findByComplaint_Id(Integer complaintId);
}
