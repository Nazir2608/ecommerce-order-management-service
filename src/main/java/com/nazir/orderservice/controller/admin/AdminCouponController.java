package com.nazir.orderservice.controller.admin;

import com.nazir.orderservice.dto.request.CreateCouponRequest;
import com.nazir.orderservice.dto.request.UpdateCouponRequest;
import com.nazir.orderservice.dto.response.CouponResponse;
import com.nazir.orderservice.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Coupons", description = "Admin coupon management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminCouponController {

    private final CouponService couponService;

    @PostMapping
    @Operation(summary = "Create coupon")
    public ResponseEntity<CouponResponse> createCoupon(@Valid @RequestBody CreateCouponRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(couponService.createCoupon(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update coupon")
    public ResponseEntity<CouponResponse> updateCoupon(@PathVariable UUID id, @Valid @RequestBody UpdateCouponRequest request) {
        return ResponseEntity.ok(couponService.updateCoupon(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate coupon")
    public ResponseEntity<Void> deactivateCoupon(@PathVariable UUID id) {
        couponService.deactivateCoupon(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get coupon by ID")
    public ResponseEntity<CouponResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(couponService.getById(id));
    }

    @GetMapping
    @Operation(summary = "Get all coupons")
    public ResponseEntity<List<CouponResponse>> getAll() {
        return ResponseEntity.ok(couponService.getAll());
    }
}