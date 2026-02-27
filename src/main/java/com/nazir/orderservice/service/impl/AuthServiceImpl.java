package com.nazir.orderservice.service.impl;

import com.nazir.orderservice.dto.request.LoginRequest;
import com.nazir.orderservice.dto.request.RefreshTokenRequest;
import com.nazir.orderservice.dto.request.RegisterRequest;
import com.nazir.orderservice.dto.response.AuthResponse;
import com.nazir.orderservice.dto.response.UserResponse;
import com.nazir.orderservice.event.UserRegisteredEvent;
import com.nazir.orderservice.entity.User;
import com.nazir.orderservice.enums.Role;
import com.nazir.orderservice.exception.DuplicateResourceException;
import com.nazir.orderservice.exception.ResourceNotFoundException;
import com.nazir.orderservice.repository.UserRepository;
import com.nazir.orderservice.security.JwtUtil;
import com.nazir.orderservice.service.AuthService;
import com.nazir.orderservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User already exists with email: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(Role.CUSTOMER)
                .build();

        userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        eventPublisher.publishEvent(new UserRegisteredEvent(user.getId()));

        return generateTokens(user.getEmail());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        log.info("User logged in: {}", user.getEmail());
        return generateTokens(user.getEmail());
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshKey = REFRESH_TOKEN_PREFIX + request.getRefreshToken();
        String email = redisTemplate.opsForValue().get(refreshKey);

        if (email == null) {
            throw new ResourceNotFoundException("Refresh token is invalid or expired");
        }

        // Rotate: delete old refresh token
        redisTemplate.delete(refreshKey);

        return generateTokens(email);
    }

    @Override
    public void logout(String accessToken) {
        if (accessToken == null) return;

        // Blacklist access token until it expires (1 hour)
        redisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + accessToken,
                "blacklisted",
                jwtUtil.getAccessTokenExpiryMs(),
                TimeUnit.MILLISECONDS
        );
    }

    private AuthResponse generateTokens(String email) {
        String accessToken = jwtUtil.generateAccessToken(email);
        String refreshToken = jwtUtil.generateRefreshToken();

        // Store refresh token in Redis for 7 days
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + refreshToken,
                email,
                7,
                TimeUnit.DAYS
        );

        UserResponse user = userRepository.findByEmail(email)
                .map(u -> UserResponse.builder()
                        .id(u.getId())
                        .name(u.getName())
                        .email(u.getEmail())
                        .phone(u.getPhone())
                        .role(u.getRole())
                        .active(Boolean.TRUE.equals(u.getIsActive()))
                        .createdAt(u.getCreatedAt())
                        .build())
                .orElse(null);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiryMs())
                .user(user)
                .build();
    }
}
