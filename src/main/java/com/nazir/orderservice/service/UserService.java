package com.nazir.orderservice.service;

import com.nazir.orderservice.dto.response.PageResponse;
import com.nazir.orderservice.dto.response.UserResponse;

import java.util.UUID;

public interface UserService {

    UserResponse getCurrentUser(UUID userId);

    UserResponse updateProfile(UUID userId, String name, String phone);

    // Admin operations
    PageResponse<UserResponse> getAllUsers(int page, int size);

    UserResponse getUserById(UUID id);

    void deactivateUser(UUID id);

    void activateUser(UUID id);
}
