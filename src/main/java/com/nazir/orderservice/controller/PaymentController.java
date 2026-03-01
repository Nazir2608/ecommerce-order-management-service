package com.nazir.orderservice.controller;

import com.nazir.orderservice.dto.response.PaymentResponse;
import com.nazir.orderservice.entity.User;
import com.nazir.orderservice.repository.UserRepository;
import com.nazir.orderservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment processing APIs")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    // ── Customer endpoints ────────────────────────────────────────────────────

    @PostMapping("/api/v1/payments/initiate")
    @Operation(summary = "Initiate payment for an order", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PaymentResponse> initiatePayment(@AuthenticationPrincipal UserDetails userDetails, @RequestParam UUID orderId) {
        return ResponseEntity.ok(paymentService.initiatePayment(getUserId(userDetails), orderId));
    }

    @PostMapping("/api/v1/payments/confirm")
    @Operation(summary = "Confirm payment (for testing — in prod use webhook)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PaymentResponse> confirmPayment(@RequestParam UUID orderId, @RequestParam(defaultValue = "true") boolean success, @RequestParam(defaultValue = "txn_test_123") String transactionId) {
        return ResponseEntity.ok(paymentService.confirmPayment(orderId, success, transactionId));
    }

    @GetMapping("/api/v1/payments/order/{orderId}")
    @Operation(summary = "Get payment details for an order", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PaymentResponse> getPaymentByOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId));
    }

    // ── Stripe webhook — NO auth, verified by signature ───────────────────────

    @PostMapping("/api/v1/payments/webhook/stripe")
    @Operation(summary = "Stripe webhook endpoint — do not call manually")
    public ResponseEntity<Void> stripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        paymentService.handleStripeWebhook(payload, sigHeader);
        return ResponseEntity.ok().build();
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @PostMapping("/api/v1/admin/payments/{paymentId}/refund")
    @Operation(summary = "Refund a payment (Admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PaymentResponse> refundPayment(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentService.processRefund(paymentId));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private UUID getUserId(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return user.getId();
    }
}