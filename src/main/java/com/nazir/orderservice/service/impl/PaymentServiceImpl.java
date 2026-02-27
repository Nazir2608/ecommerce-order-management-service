package com.nazir.orderservice.service.impl;

import com.nazir.orderservice.dto.response.PaymentResponse;
import com.nazir.orderservice.entity.Order;
import com.nazir.orderservice.entity.Payment;
import com.nazir.orderservice.enums.OrderStatus;
import com.nazir.orderservice.enums.PaymentMethod;
import com.nazir.orderservice.enums.PaymentStatus;
import com.nazir.orderservice.exception.PaymentException;
import com.nazir.orderservice.exception.ResourceNotFoundException;
import com.nazir.orderservice.repository.OrderRepository;
import com.nazir.orderservice.repository.PaymentRepository;
import com.nazir.orderservice.service.NotificationService;
import com.nazir.orderservice.service.OrderService;
import com.nazir.orderservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    @Value("${app.stripe.mock-enabled:true}")
    private boolean stripeMock;

    @Value("${app.stripe.secret-key:sk_test_placeholder}")
    private String stripeSecretKey;

    @Override
    @Transactional
    public PaymentResponse initiatePayment(UUID userId, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new PaymentException("Access denied");
        }

        // Check if payment already exists
        paymentRepository.findByOrderId(orderId).ifPresent(p -> {
            if (p.getPaymentStatus() == PaymentStatus.SUCCESS) {
                throw new PaymentException("Payment already completed for order: " + orderId);
            }
        });

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElse(Payment.builder()
                        .order(order)
                        .paymentMethod(PaymentMethod.CREDIT_CARD)
                        .paymentStatus(PaymentStatus.PENDING)
                        .amount(order.getFinalAmount())
                        .currency("USD")
                        .build());

        String clientSecret;
        if (stripeMock) {
            // Mock mode: return a fake client secret
            clientSecret = "pi_mock_" + UUID.randomUUID().toString().replace("-", "") + "_secret_mock";
            log.info("[MOCK STRIPE] PaymentIntent created for order: {} amount: {}", orderId, order.getFinalAmount());
        } else {
            // Real Stripe integration
            // Stripe.apiKey = stripeSecretKey;
            // PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
            //     .setAmount(order.getFinalAmount().multiply(BigDecimal.valueOf(100)).longValue())
            //     .setCurrency("usd")
            //     .setMetadata(Map.of("orderId", orderId.toString()))
            //     .build();
            // PaymentIntent intent = PaymentIntent.create(params);
            // clientSecret = intent.getClientSecret();
            clientSecret = "real_stripe_client_secret";
        }

        payment = paymentRepository.save(payment);

        return toResponse(payment, clientSecret);
    }

    @Override
    @Transactional
    public PaymentResponse confirmPayment(UUID orderId, boolean success, String transactionId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order: " + orderId));

        Order order = payment.getOrder();

        if (success) {
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(transactionId);
            payment.setPaidAt(LocalDateTime.now());
            payment.setGatewayResponse("{\"status\":\"succeeded\",\"id\":\"" + transactionId + "\"}");

            // Update order status to CONFIRMED
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);

            notificationService.sendPaymentSuccessNotification(order);
            log.info("Payment confirmed for order: {} txn: {}", orderId, transactionId);
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setGatewayResponse("{\"status\":\"failed\"}");
            notificationService.sendPaymentFailedNotification(order);
            log.warn("Payment failed for order: {}", orderId);
        }

        payment = paymentRepository.save(payment);
        return toResponse(payment, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order: " + orderId));
        return toResponse(payment, null);
    }

    @Override
    @Transactional
    public PaymentResponse processRefund(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new PaymentException("Can only refund successful payments");
        }

        if (stripeMock) {
            log.info("[MOCK STRIPE] Refund processed for payment: {} amount: {}",
                    paymentId, payment.getAmount());
        }
        // Real Stripe: Refund.create(RefundCreateParams.builder().setPaymentIntent(...).build())

        payment.setPaymentStatus(PaymentStatus.REFUNDED);
        payment.getOrder().setStatus(OrderStatus.REFUNDED);
        orderRepository.save(payment.getOrder());

        notificationService.sendRefundProcessedNotification(payment.getOrder());
        payment = paymentRepository.save(payment);

        return toResponse(payment, null);
    }

    @Override
    public void handleStripeWebhook(String payload, String sigHeader) {
        // In real implementation: verify webhook signature using Stripe SDK
        // Event event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);
        log.info("Stripe webhook received (mock handling)");
    }

    private PaymentResponse toResponse(Payment payment, String clientSecret) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .transactionId(payment.getTransactionId())
                .paidAt(payment.getPaidAt())
                .clientSecret(clientSecret)
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
