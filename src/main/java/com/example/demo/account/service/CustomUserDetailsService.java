package com.example.demo.account.service;

import com.example.demo.account.entity.LocalAccount;
import com.example.demo.account.entity.User;
import com.example.demo.account.repository.LocalAccountRepository;
import com.example.demo.account.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final LocalAccountRepository localAccountRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));

        LocalAccount local = localAccountRepository.findById(user.getUserId())
                .orElseThrow(() -> new UsernameNotFoundException(email));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(local.getPasswordHash())
                .roles(user.getRole().name().toUpperCase())
                .disabled(!user.getIsActive())
                .build();
    }
}
