package com.nazir.orderservice.controller;

import com.nazir.orderservice.dto.request.AddressRequest;
import com.nazir.orderservice.dto.request.UpdateProfileRequest;
import com.nazir.orderservice.dto.response.AddressResponse;
import com.nazir.orderservice.dto.response.OrderSummaryResponse;
import com.nazir.orderservice.dto.response.PageResponse;
import com.nazir.orderservice.dto.response.UserResponse;
import com.nazir.orderservice.entity.User;
import com.nazir.orderservice.repository.UserRepository;
import com.nazir.orderservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "User profile, address and order management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping("/me")
    @Operation(summary = "Get my profile (includes addresses)")
    public ResponseEntity<UserResponse> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getCurrentUser(getUserId(userDetails)));
    }

    @PutMapping("/me")
    @Operation(summary = "Update my profile (name, phone)")
    public ResponseEntity<UserResponse> updateProfile(@AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(getUserId(userDetails), request.getName(), request.getPhone()));
    }

    @PostMapping("/me/addresses")
    @Operation(summary = "Add a new shipping address")
    public ResponseEntity<AddressResponse> addAddress(@AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody AddressRequest request) {
        AddressResponse response = userService.addAddress(getUserId(userDetails), request.getStreet(), request.getCity(), request.getState(), request.getCountry(), request.getZipCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me/addresses")
    @Operation(summary = "Get all my addresses")
    public ResponseEntity<List<AddressResponse>> getMyAddresses(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getMyAddresses(getUserId(userDetails)));
    }

    @PutMapping("/me/addresses/{addressId}")
    @Operation(summary = "Update an address")
    public ResponseEntity<AddressResponse> updateAddress(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID addressId, @Valid @RequestBody AddressRequest request) {
        AddressResponse response = userService.updateAddress(getUserId(userDetails), addressId, request.getStreet(), request.getCity(), request.getState(), request.getCountry(), request.getZipCode());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me/addresses/{addressId}/default")
    @Operation(summary = "Set an address as default")
    public ResponseEntity<AddressResponse> setDefaultAddress(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID addressId) {
        return ResponseEntity.ok(userService.setDefaultAddress(getUserId(userDetails), addressId));
    }

    @DeleteMapping("/me/addresses/{addressId}")
    @Operation(summary = "Delete an address")
    public ResponseEntity<Void> deleteAddress(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID addressId) {
        userService.deleteAddress(getUserId(userDetails), addressId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/orders")
    @Operation(summary = "Get my order history")
    public ResponseEntity<PageResponse<OrderSummaryResponse>> getMyOrders(@AuthenticationPrincipal UserDetails userDetails, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(userService.getMyOrders(getUserId(userDetails), page, size));
    }

    private UUID getUserId(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return user.getId();
    }
}