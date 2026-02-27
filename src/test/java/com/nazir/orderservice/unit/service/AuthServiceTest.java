package com.nazir.orderservice.unit.service;

import com.nazir.orderservice.dto.request.LoginRequest;
import com.nazir.orderservice.dto.request.RegisterRequest;
import com.nazir.orderservice.dto.response.AuthResponse;
import com.nazir.orderservice.entity.User;
import com.nazir.orderservice.enums.Role;
import com.nazir.orderservice.exception.DuplicateResourceException;
import com.nazir.orderservice.repository.UserRepository;
import com.nazir.orderservice.security.JwtUtil;
import com.nazir.orderservice.service.NotificationService;
import com.nazir.orderservice.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil tokenProvider;
    @Mock AuthenticationManager authenticationManager;
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;
    @Mock NotificationService notificationService;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks
    AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setName("John Doe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPassword("Password1");
        registerRequest.setPhone("+1234567890");

        savedUser = User.builder()
            .name("John Doe")
            .email("john@example.com")
            .password("encoded_password")
            .role(Role.CUSTOMER)
            .isActive(true)
            .build();
    }

    @Test
    @DisplayName("register_WithValidData_ShouldReturnAuthResponse")
    void register_WithValidData_ShouldReturnAuthResponse() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(savedUser));
        when(tokenProvider.generateAccessToken(anyString())).thenReturn("access_token");
        when(tokenProvider.getAccessTokenExpiryMs()).thenReturn(3600000L);
        when(tokenProvider.generateRefreshToken()).thenReturn("refresh_token");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        AuthResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access_token");
        assertThat(response.getUser().getEmail()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("register_WithDuplicateEmail_ShouldThrowException")
    void register_WithDuplicateEmail_ShouldThrowException() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("User already exists");

        verify(userRepository, never()).save(any());
    }
}
