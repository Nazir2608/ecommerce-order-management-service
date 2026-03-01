package com.nazir.orderservice.service;
import com.nazir.orderservice.dto.response.AddressResponse;
import com.nazir.orderservice.dto.response.OrderSummaryResponse;
import com.nazir.orderservice.dto.response.PageResponse;
import com.nazir.orderservice.dto.response.UserResponse;
import java.util.List;
import java.util.UUID;

public interface UserService {
    UserResponse getCurrentUser(UUID userId);
    UserResponse updateProfile(UUID userId, String name, String phone);
    AddressResponse addAddress(UUID userId, String street, String city, String state, String country, String zipCode);
    List<AddressResponse> getMyAddresses(UUID userId);
    AddressResponse updateAddress(UUID userId, UUID addressId, String street, String city, String state, String country, String zipCode);
    void deleteAddress(UUID userId, UUID addressId);
    AddressResponse setDefaultAddress(UUID userId, UUID addressId);
    PageResponse<OrderSummaryResponse> getMyOrders(UUID userId, int page, int size);

    // ── Admin ──
    PageResponse<UserResponse> getAllUsers(int page, int size);
    UserResponse getUserById(UUID id);
    void deactivateUser(UUID id);
    void activateUser(UUID id);
}