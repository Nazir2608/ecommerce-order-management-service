package com.nazir.orderservice.service.impl;

import com.nazir.orderservice.entity.Notification;
import com.nazir.orderservice.entity.Order;
import com.nazir.orderservice.entity.Payment;
import com.nazir.orderservice.entity.User;
import com.nazir.orderservice.enums.NotificationStatus;
import com.nazir.orderservice.enums.NotificationType;
import com.nazir.orderservice.repository.NotificationRepository;
import com.nazir.orderservice.repository.PaymentRepository;
import com.nazir.orderservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final JavaMailSender         mailSender;
    private final TemplateEngine         templateEngine;
    private final NotificationRepository notificationRepository;
    private final PaymentRepository      paymentRepository;

    @Value("${app.mail.from:noreply@orderflow.com}")
    private String fromEmail;

    @Value("${app.mail.from-name:OrderFlow}")
    private String fromName;

    @Value("${app.twilio.mock-enabled:true}")
    private boolean twilioMock;

    @Override
    @Async
    public void sendWelcomeEmail(User user) {
        Context ctx = new Context();
        ctx.setVariable("name",      user.getName());
        ctx.setVariable("email",     user.getEmail());
        ctx.setVariable("joinedAt",  user.getCreatedAt());
        sendEmail(user.getEmail(), "Welcome to OrderFlow! 🎉", "email/welcome", ctx, user, "USER_REGISTERED");
    }

    @Override
    @Async
    public void sendOrderPlacedNotification(Order order) {
        Context ctx = new Context();
        ctx.setVariable("name",            order.getUser().getName());
        ctx.setVariable("orderNumber",     order.getOrderNumber());
        ctx.setVariable("items",           order.getItems());
        ctx.setVariable("totalAmount",     order.getTotalAmount());
        ctx.setVariable("discountAmount",  order.getDiscountAmount());
        ctx.setVariable("shippingAmount",  order.getShippingAmount());
        ctx.setVariable("finalAmount",     order.getFinalAmount());
        ctx.setVariable("shippingAddress", order.getShippingAddress());
        ctx.setVariable("notes",           order.getNotes());
        ctx.setVariable("placedAt",        order.getCreatedAt());

        sendEmail(order.getUser().getEmail(), "Order Confirmed: " + order.getOrderNumber(), "email/order-placed", ctx, order.getUser(), "ORDER_PLACED");

        if (order.getUser().getPhone() != null) {
            sendSms(order.getUser().getPhone(), "Your order " + order.getOrderNumber() + " placed. Total: ₹" + order.getFinalAmount(), order.getUser(), "ORDER_PLACED");
        }
    }


    @Override
    @Async
    public void sendPaymentSuccessNotification(Order order) {
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        Context ctx = new Context();
        ctx.setVariable("name",          order.getUser().getName());
        ctx.setVariable("orderNumber",   order.getOrderNumber());
        ctx.setVariable("amount",        order.getFinalAmount());
        ctx.setVariable("currency",      payment != null ? payment.getCurrency() : "INR");
        ctx.setVariable("paymentMethod", payment != null ? payment.getPaymentMethod().name() : "");
        ctx.setVariable("transactionId", payment != null ? payment.getTransactionId() : "");
        ctx.setVariable("paidAt",        payment != null ? payment.getPaidAt() : LocalDateTime.now());
        ctx.setVariable("items",         order.getItems());
        ctx.setVariable("shippingAddress", order.getShippingAddress());

        sendEmail(order.getUser().getEmail(), "Payment Successful - " + order.getOrderNumber(), "email/payment-success", ctx, order.getUser(), "PAYMENT_SUCCESS");
    }

    @Override
    @Async
    public void sendPaymentFailedNotification(Order order) {
        Context ctx = new Context();
        ctx.setVariable("name",        order.getUser().getName());
        ctx.setVariable("orderNumber", order.getOrderNumber());
        ctx.setVariable("amount",      order.getFinalAmount());
        ctx.setVariable("failedAt",    LocalDateTime.now());

        sendEmail(order.getUser().getEmail(), "Payment Failed - " + order.getOrderNumber(), "email/payment-failed", ctx, order.getUser(), "PAYMENT_FAILED");
    }

    @Override
    @Async
    public void sendOrderStatusUpdatedNotification(Order order) {
        Context ctx = new Context();
        ctx.setVariable("name",        order.getUser().getName());
        ctx.setVariable("orderNumber", order.getOrderNumber());
        ctx.setVariable("status",      order.getStatus().name());
        ctx.setVariable("updatedAt",   order.getUpdatedAt());

        sendEmail(order.getUser().getEmail(), "Order Update: " + order.getOrderNumber() + " is " + order.getStatus(), "email/order-status-update", ctx, order.getUser(), "ORDER_STATUS_UPDATED");
    }


    @Override
    @Async
    public void sendOrderCancelledNotification(Order order) {
        Context ctx = new Context();
        ctx.setVariable("name",        order.getUser().getName());
        ctx.setVariable("orderNumber", order.getOrderNumber());
        ctx.setVariable("amount",      order.getFinalAmount());
        ctx.setVariable("items",       order.getItems());
        ctx.setVariable("cancelledAt", LocalDateTime.now());

        sendEmail(order.getUser().getEmail(), "Order Cancelled: " + order.getOrderNumber(), "email/order-cancelled", ctx, order.getUser(), "ORDER_CANCELLED");
    }


    @Override
    @Async
    public void sendRefundProcessedNotification(Order order) {
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);

        Context ctx = new Context();
        ctx.setVariable("name",          order.getUser().getName());
        ctx.setVariable("orderNumber",   order.getOrderNumber());
        ctx.setVariable("amount",        order.getFinalAmount());
        ctx.setVariable("transactionId", payment != null ? payment.getTransactionId() : "");
        ctx.setVariable("refundedAt",    LocalDateTime.now());

        sendEmail(order.getUser().getEmail(), "Refund Processed: " + order.getOrderNumber(), "email/refund-processed", ctx, order.getUser(), "REFUND_PROCESSED");
    }


    private void sendEmail(String to, String subject, String template, Context ctx, User user, String event) {
        Notification notification = Notification.builder()
                .user(user)
                .type(NotificationType.EMAIL)
                .event(event)
                .recipient(to)
                .subject(subject)
                .status(NotificationStatus.PENDING)
                .build();

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                String body = templateEngine.process(template, ctx);
                notification.setBody(body);

                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromEmail, fromName);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(body, true);
                mailSender.send(message);

                notification.setStatus(NotificationStatus.SENT);
                notificationRepository.save(notification);
                log.info("Email sent to={} event={}", to, event);
                return;

            } catch (Exception e) {
                log.warn("Email attempt {}/3 failed for {}: {}", attempt, to, e.getMessage());
                if (attempt == 3) {
                    notification.setStatus(NotificationStatus.FAILED);
                    notificationRepository.save(notification);
                    log.error("All 3 email attempts failed for {} event={}", to, event);
                }
            }
        }
    }


    private void sendSms(String phone, String message, User user, String event) {
        Notification notification = Notification.builder()
                .user(user)
                .type(NotificationType.SMS)
                .event(event)
                .recipient(phone)
                .subject("SMS")
                .body(message)
                .status(NotificationStatus.PENDING)
                .build();
        try {
            if (twilioMock) {
                log.info("[MOCK SMS] To={} | {}", phone, message);
            }
            // Real Twilio: Message.creator(new PhoneNumber(phone), new PhoneNumber(fromNumber), message).create();
            notification.setStatus(NotificationStatus.SENT);
        } catch (Exception e) {
            log.error("SMS failed to {}: {}", phone, e.getMessage());
            notification.setStatus(NotificationStatus.FAILED);
        } finally {
            notificationRepository.save(notification);
        }
    }
}