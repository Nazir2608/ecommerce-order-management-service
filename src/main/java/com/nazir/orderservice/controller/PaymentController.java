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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment processing APIs")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    @PostMapping("/payments/initiate")
    @Operation(summary = "Initiate payment for an order")
    public ResponseEntity<PaymentResponse> initiatePayment(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam UUID orderId) {
        return ResponseEntity.ok(paymentService.initiatePayment(getUserId(userDetails), orderId));
    }

    @PostMapping("/payments/confirm")
    @Operation(summary = "Confirm payment (mock/testing endpoint)")
    public ResponseEntity<PaymentResponse> confirmPayment(
            @RequestParam UUID orderId,
            @RequestParam(defaultValue = "true") boolean success,
            @RequestParam(defaultValue = "txn_mock_123") String transactionId) {
        return ResponseEntity.ok(paymentService.confirmPayment(orderId, success, transactionId));
    }

    @GetMapping("/payments/order/{orderId}")
    @Operation(summary = "Get payment details for an order")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID orderId) {
        return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId));
    }

    @PostMapping("/admin/payments/{paymentId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Process refund for a payment")
    public ResponseEntity<PaymentResponse> processRefund(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentService.processRefund(paymentId));
    }

    @PostMapping("/payments/webhook")
    @Operation(summary = "Stripe webhook endpoint (public)")
    public ResponseEntity<Void> handleWebhook(@RequestBody String payload, @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        paymentService.handleStripeWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }

    private UUID getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername()).orElseThrow().getId();
    }
}
