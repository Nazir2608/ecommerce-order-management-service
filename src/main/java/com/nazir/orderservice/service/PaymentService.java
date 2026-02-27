package com.nazir.orderservice.service;

import com.nazir.orderservice.dto.response.PaymentResponse;

import java.util.Map;
import java.util.UUID;

public interface PaymentService {
    PaymentResponse initiatePayment(UUID userId, UUID orderId);
    PaymentResponse confirmPayment(UUID orderId, boolean success, String transactionId);
    PaymentResponse getPaymentByOrderId(UUID orderId);
    PaymentResponse processRefund(UUID paymentId);
    void handleStripeWebhook(String payload, String sigHeader);
}
