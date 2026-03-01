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
import com.nazir.orderservice.service.PaymentService;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    @Value("${app.stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${app.stripe.webhook-secret}")
    private String stripeWebhookSecret;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
        log.info("Stripe SDK initialized");
    }

    @Override
    @Transactional
    public PaymentResponse initiatePayment(UUID userId, UUID orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        if (!order.getUser().getId().equals(userId)) {
            throw new PaymentException("Access denied");
        }
        // Block if already paid
        paymentRepository.findByOrderId(orderId).ifPresent(p -> {
            if (p.getPaymentStatus() == PaymentStatus.SUCCESS) {
                throw new PaymentException("Payment already completed for order: " + orderId);
            }
        });
        // Get existing pending payment or create new one
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElse(Payment.builder()
                        .order(order)
                        .paymentMethod(PaymentMethod.CREDIT_CARD)
                        .paymentStatus(PaymentStatus.PENDING)
                        .amount(order.getFinalAmount())
                        .currency("INR")
                        .build());

        try {
            // Stripe requires amount in smallest currency unit (paise for INR)
            long amountInPaise = order.getFinalAmount().multiply(BigDecimal.valueOf(100)).longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInPaise)
                    .setCurrency("inr")
                    .setDescription("Order #" + order.getOrderNumber())
                    .putMetadata("orderId", orderId.toString())
                    .putMetadata("userId", userId.toString())
                    .putMetadata("orderNumber", order.getOrderNumber())
                    .setAutomaticPaymentMethods(PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build())
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);
            // Save Stripe PaymentIntent ID — needed for refunds and webhook matching
            payment.setTransactionId(intent.getId());
            payment = paymentRepository.save(payment);

            log.info("Stripe PaymentIntent created: {} for order={}", intent.getId(), orderId);
            return toResponse(payment, intent.getClientSecret());

        } catch (StripeException e) {
            log.error("Stripe PaymentIntent creation failed for order={}: {}", orderId, e.getMessage());
            throw new PaymentException("Failed to initiate payment: " + e.getMessage());
        }
    }

    // CONFIRM PAYMENT — for dev testing only, production uses webhook
    @Override
    @Transactional
    public PaymentResponse confirmPayment(UUID orderId, boolean success, String transactionId) {
        Payment payment = paymentRepository.findByOrderId(orderId).orElseThrow(() -> new ResourceNotFoundException("Payment not found for order: " + orderId));
        Order order = payment.getOrder();

        if (success) {
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(transactionId);
            payment.setPaidAt(LocalDateTime.now());
            payment.setGatewayResponse("{\"status\":\"succeeded\",\"id\":\"" + transactionId + "\"}");
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
            notificationService.sendPaymentSuccessNotification(order);
            log.info("Payment confirmed for order={} txn={}", orderId, transactionId);
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setGatewayResponse("{\"status\":\"failed\"}");
            notificationService.sendPaymentFailedNotification(order);
            log.warn("Payment failed for order={}", orderId);
        }
        payment = paymentRepository.save(payment);
        return toResponse(payment, null);
    }

    // STRIPE WEBHOOK — Stripe calls this after payment is processed
    @Override
    @Transactional
    public void handleStripeWebhook(String payload, String sigHeader) {
        // Verify signature — prevents forged webhook calls
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Invalid Stripe webhook signature: {}", e.getMessage());
            throw new PaymentException("Invalid webhook signature");
        }

        log.info("Stripe webhook received: type={} id={}", event.getType(), event.getId());

        switch (event.getType()) {

            case "payment_intent.succeeded" -> {
                EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
                if (deserializer.getObject().isPresent()) {
                    PaymentIntent intent = (PaymentIntent) deserializer.getObject().get();
                    String orderId = intent.getMetadata().get("orderId");
                    if (orderId != null) {
                        confirmPayment(UUID.fromString(orderId), true, intent.getId());
                        log.info("Webhook: payment succeeded for order={}", orderId);
                    }
                }
            }

            case "payment_intent.payment_failed" -> {
                EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
                if (deserializer.getObject().isPresent()) {
                    PaymentIntent intent = (PaymentIntent) deserializer.getObject().get();
                    String orderId = intent.getMetadata().get("orderId");
                    if (orderId != null) {
                        confirmPayment(UUID.fromString(orderId), false, intent.getId());
                        log.warn("Webhook: payment failed for order={}", orderId);
                    }
                }
            }

            default -> log.info("Unhandled Stripe event: {}", event.getType());
        }
    }

    @Override
    @Transactional
    public PaymentResponse processRefund(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new PaymentException("Can only refund successful payments");
        }
        try {
            RefundCreateParams params = RefundCreateParams.builder().setPaymentIntent(payment.getTransactionId()).build();
            Refund refund = Refund.create(params);
            payment.setGatewayResponse("{\"refundId\":\"" + refund.getId() + "\"}");
            log.info("Stripe refund created: {} for paymentId={}", refund.getId(), paymentId);
        } catch (StripeException e) {
            log.error("Stripe refund failed for paymentId={}: {}", paymentId, e.getMessage());
            throw new PaymentException("Refund failed: " + e.getMessage());
        }

        payment.setPaymentStatus(PaymentStatus.REFUNDED);
        payment.getOrder().setStatus(OrderStatus.REFUNDED);
        orderRepository.save(payment.getOrder());

        notificationService.sendRefundProcessedNotification(payment.getOrder());
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