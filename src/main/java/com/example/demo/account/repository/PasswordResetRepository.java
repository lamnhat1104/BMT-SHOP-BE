package com.example.demo.account.repository;

import com.example.demo.account.entity.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, Integer> {
    Optional<PasswordReset> findByToken(String token);
    void deleteByEmail(String email);
}
