package com.nazir.orderservice.controller.admin;

import com.nazir.orderservice.dto.request.UpdateOrderStatusRequest;
import com.nazir.orderservice.dto.response.OrderResponse;
import com.nazir.orderservice.dto.response.PageResponse;
import com.nazir.orderservice.enums.OrderStatus;
import com.nazir.orderservice.service.OrderService;
import com.nazir.orderservice.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(value = "features.admin.orders.enabled", havingValue = "true", matchIfMissing = false)
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Orders", description = "Admin order management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminOrderController {

    private final OrderService orderService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "Get all orders with filters")
    public ResponseEntity<PageResponse<OrderResponse>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @org.springframework.data.web.PageableDefault(size = 20, sort = "createdAt") org.springframework.data.domain.Pageable pageable) {

        UUID adminId = securityUtils.getCurrentUserId();
        String fromStr = from != null ? from.toString() : null;
        String toStr = to != null ? to.toString() : null;
        return ResponseEntity.ok(orderService.getAllOrders(adminId, status, fromStr, toStr, pageable));
    }

    @PatchMapping("/{orderId}/status")
    @Operation(summary = "Update order status")
    public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable UUID orderId, @RequestBody UpdateOrderStatusRequest request) {
        UUID adminId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(orderService.updateOrderStatus(adminId, orderId, request.getNewStatus(), request.getReason()));
    }
}
