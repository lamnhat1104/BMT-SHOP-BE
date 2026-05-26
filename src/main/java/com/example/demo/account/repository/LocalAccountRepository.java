package com.example.demo.account.repository;

import com.example.demo.account.entity.LocalAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalAccountRepository extends JpaRepository<LocalAccount, Integer> {
}
