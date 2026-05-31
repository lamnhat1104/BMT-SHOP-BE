package com.example.demo.account.service;

import com.example.demo.account.dto.UserResponse;
import com.example.demo.account.dto.UserSaveRequest;
import com.example.demo.account.entity.LocalAccount;
import com.example.demo.account.entity.User;
import com.example.demo.account.repository.LocalAccountRepository;
import com.example.demo.account.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminUserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LocalAccountRepository localAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private User sampleUser;
    private UserSaveRequest saveRequest;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setUserId(1);
        sampleUser.setFullName("Test User");
        sampleUser.setEmail("test@gmail.com");
        sampleUser.setPhone("0987654321");
        sampleUser.setAddress("Hanoi");
        sampleUser.setRole(User.Role.member);
        sampleUser.setIsActive(true);
        sampleUser.setCreatedAt(LocalDateTime.now());

        saveRequest = UserSaveRequest.builder()
                .fullName("Test User")
                .email("test@gmail.com")
                .phone("0987654321")
                .address("Hanoi")
                .role("member")
                .build();
    }

    @Test
    void getAllUsers_ShouldReturnList() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser));

        List<UserResponse> result = adminUserService.getAllUsers();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test User", result.get(0).getFullName());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void createUser_UniqueEmail_ShouldSaveUserAndLocalAccount() {
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(passwordEncoder.encode("123456")).thenReturn("hashedPassword");
        when(localAccountRepository.save(any(LocalAccount.class))).thenReturn(new LocalAccount());

        UserResponse result = adminUserService.createUser(saveRequest);

        assertNotNull(result);
        assertEquals("test@gmail.com", result.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
        verify(localAccountRepository, times(1)).save(any(LocalAccount.class));
    }

    @Test
    void createUser_DuplicateEmail_ShouldThrowException() {
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(sampleUser));

        assertThrows(RuntimeException.class, () -> adminUserService.createUser(saveRequest));
        verify(userRepository, never()).save(any(User.class));
        verify(localAccountRepository, never()).save(any(LocalAccount.class));
    }

    @Test
    void toggleUserStatus_ShouldToggleActiveStatus() {
        when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse result = adminUserService.toggleUserStatus(1);

        assertNotNull(result);
        assertEquals(0, result.getIsActive()); // From true to false
        verify(userRepository, times(1)).save(any(User.class));
    }
}
