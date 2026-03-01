package com.nazir.orderservice.controller;

import com.nazir.orderservice.dto.request.PlaceOrderRequest;
import com.nazir.orderservice.dto.request.UpdateOrderStatusRequest;
import com.nazir.orderservice.dto.response.OrderResponse;
import com.nazir.orderservice.dto.response.PageResponse;
import com.nazir.orderservice.entity.User;
import com.nazir.orderservice.enums.OrderStatus;
import com.nazir.orderservice.repository.UserRepository;
import com.nazir.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order placement and management")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    // ===== CUSTOMER ENDPOINTS =====

    @PostMapping("/api/v1/orders")
    @Operation(summary = "Place a new order from cart")
    public ResponseEntity<OrderResponse> placeOrder(@AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody PlaceOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(getUserId(userDetails), request));
    }

    @GetMapping("/api/v1/orders")
    @Operation(summary = "Get my orders")
    public ResponseEntity<PageResponse<OrderResponse>> getMyOrders(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) OrderStatus status, @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(orderService.getMyOrders(getUserId(userDetails), status, pageable));
    }

    @GetMapping("/api/v1/orders/{orderId}")
    @Operation(summary = "Get order details")
    public ResponseEntity<OrderResponse> getOrder(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getOrderById(getUserId(userDetails), orderId));
    }

    @PostMapping("/api/v1/orders/{orderId}/cancel")
    @Operation(summary = "Cancel an order")
    public ResponseEntity<OrderResponse> cancelOrder(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.cancelOrder(getUserId(userDetails), orderId));
    }

    // ===== ADMIN ENDPOINTS =====

    @GetMapping("/api/v1/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Get all orders with filters")
    public ResponseEntity<PageResponse<OrderResponse>> getAllOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(orderService.getAllOrders(getUserId(userDetails), status, from, to, pageable));
    }

    @PatchMapping("/api/v1/admin/orders/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Update order status")
    public ResponseEntity<OrderResponse> updateStatus(@AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID orderId, @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(getUserId(userDetails), orderId, request.getNewStatus(), request.getReason()));
    }

    private UUID getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername()).orElseThrow().getId();
    }
}
