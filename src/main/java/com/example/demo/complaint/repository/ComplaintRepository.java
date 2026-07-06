package com.example.demo.complaint.repository;

import com.example.demo.complaint.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Integer> {
    List<Complaint> findByUser_UserIdOrderByCreatedAtDesc(Integer userId);
    List<Complaint> findByOrder_Id(Integer orderId);
}
