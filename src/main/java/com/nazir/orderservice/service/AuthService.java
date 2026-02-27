package com.nazir.orderservice.service;

import com.nazir.orderservice.dto.request.LoginRequest;
import com.nazir.orderservice.dto.request.RefreshTokenRequest;
import com.nazir.orderservice.dto.request.RegisterRequest;
import com.nazir.orderservice.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(String accessToken);
}
