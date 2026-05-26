package com.example.demo.account.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "local_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LocalAccount {
    @Id
    private Integer userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String passwordHash;

    private Boolean isEmailVerified = false;
    private String verificationCode;
    private LocalDateTime codeExpiredAt;
}
