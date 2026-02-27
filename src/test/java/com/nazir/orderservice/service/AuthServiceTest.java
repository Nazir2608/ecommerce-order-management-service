package com.nazir.orderservice.service;


import com.nazir.orderservice.dto.request.RegisterRequest;
import com.nazir.orderservice.dto.request.LoginRequest;
import com.nazir.orderservice.entity.User;
import com.nazir.orderservice.enums.Role;
import com.nazir.orderservice.repository.UserRepository;
import com.nazir.orderservice.service.NotificationService;
import com.nazir.orderservice.dto.response.AuthResponse;
import com.nazir.orderservice.exception.DuplicateResourceException;
import com.nazir.orderservice.service.impl.AuthServiceImpl;
import com.nazir.orderservice.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil tokenProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private NotificationService notificationService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void register_WithValidData_ShouldReturnAuthResponse() {
        RegisterRequest request = new RegisterRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("Password123!");
        request.setPhone("+1234567890");

        User savedUser = User.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("hashed")
                .role(Role.CUSTOMER)
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(savedUser);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(savedUser));
        when(tokenProvider.generateAccessToken(any())).thenReturn("access-token");
        when(tokenProvider.getAccessTokenExpiryMs()).thenReturn(3600000L);
        when(tokenProvider.generateRefreshToken()).thenReturn("refresh-token");
        doNothing().when(notificationService).sendWelcomeEmail(any());

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_WithDuplicateEmail_ShouldThrowException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_WithValidCredentials_ShouldReturnAuthResponse() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("Password123!");

        User user = User.builder()
                .email("john@example.com")
                .password("hashed")
                .role(Role.CUSTOMER)
                .isActive(true)
                .build();

        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken("john@example.com", null));
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(tokenProvider.generateAccessToken(any())).thenReturn("access-token");
        when(tokenProvider.getAccessTokenExpiryMs()).thenReturn(3600000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
    }
}
