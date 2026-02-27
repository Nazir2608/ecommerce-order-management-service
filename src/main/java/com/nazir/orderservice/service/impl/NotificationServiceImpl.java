package com.nazir.orderservice.service.impl;

import com.nazir.orderservice.entity.Notification;
import com.nazir.orderservice.entity.Order;
import com.nazir.orderservice.entity.User;
import com.nazir.orderservice.enums.NotificationStatus;
import com.nazir.orderservice.enums.NotificationType;
import com.nazir.orderservice.repository.NotificationRepository;
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

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final NotificationRepository notificationRepository;

    @Value("${app.mail.from:noreply@orderservice.com}")
    private String fromEmail;

    @Value("${app.mail.from-name:Orderservice}")
    private String fromName;

    @Value("${app.twilio.mock-enabled:true}")
    private boolean twilioMock;

    @Override
    @Async
    public void sendWelcomeEmail(User user) {
        Context ctx = new Context();
        ctx.setVariable("name", user.getName());

        sendEmail(user.getEmail(), "Welcome to Orderservice! 🎉",
                "email/welcome", ctx, user, "USER_REGISTERED");
    }

    @Override
    @Async
    public void sendOrderPlacedNotification(Order order) {
        Context ctx = new Context();
        ctx.setVariable("name", order.getUser().getName());
        ctx.setVariable("orderNumber", order.getOrderNumber());
        ctx.setVariable("items", order.getItems());
        ctx.setVariable("totalAmount", order.getFinalAmount());

        sendEmail(order.getUser().getEmail(),
                "Order Confirmed: " + order.getOrderNumber(),
                "email/order-placed", ctx, order.getUser(), "ORDER_PLACED");

        if (order.getUser().getPhone() != null) {
            sendSms(order.getUser().getPhone(),
                    "Your order " + order.getOrderNumber() + " has been placed. Total: $" + order.getFinalAmount(),
                    order.getUser(), "ORDER_PLACED");
        }
    }

    @Override
    @Async
    public void sendPaymentSuccessNotification(Order order) {
        Context ctx = new Context();
        ctx.setVariable("name", order.getUser().getName());
        ctx.setVariable("orderNumber", order.getOrderNumber());
        ctx.setVariable("amount", order.getFinalAmount());

        sendEmail(order.getUser().getEmail(),
                "Payment Successful - " + order.getOrderNumber(),
                "email/payment-success", ctx, order.getUser(), "PAYMENT_SUCCESS");
    }

    @Override
    @Async
    public void sendPaymentFailedNotification(Order order) {
        Context ctx = new Context();
        ctx.setVariable("name", order.getUser().getName());
        ctx.setVariable("orderNumber", order.getOrderNumber());

        sendEmail(order.getUser().getEmail(),
                "Payment Failed - " + order.getOrderNumber(),
                "email/payment-failed", ctx, order.getUser(), "PAYMENT_FAILED");
    }

    @Override
    @Async
    public void sendOrderStatusUpdatedNotification(Order order) {
        Context ctx = new Context();
        ctx.setVariable("name", order.getUser().getName());
        ctx.setVariable("orderNumber", order.getOrderNumber());
        ctx.setVariable("status", order.getStatus().name());

        sendEmail(order.getUser().getEmail(),
                "Order Update: " + order.getOrderNumber() + " is " + order.getStatus(),
                "email/order-status-update", ctx, order.getUser(), "ORDER_STATUS_UPDATED");
    }

    @Override
    @Async
    public void sendOrderCancelledNotification(Order order) {
        Context ctx = new Context();
        ctx.setVariable("name", order.getUser().getName());
        ctx.setVariable("orderNumber", order.getOrderNumber());
        ctx.setVariable("amount", order.getFinalAmount());

        sendEmail(order.getUser().getEmail(),
                "Order Cancelled: " + order.getOrderNumber(),
                "email/order-cancelled", ctx, order.getUser(), "ORDER_CANCELLED");
    }

    @Override
    @Async
    public void sendRefundProcessedNotification(Order order) {
        Context ctx = new Context();
        ctx.setVariable("name", order.getUser().getName());
        ctx.setVariable("orderNumber", order.getOrderNumber());
        ctx.setVariable("amount", order.getFinalAmount());

        sendEmail(order.getUser().getEmail(),
                "Refund Processed: " + order.getOrderNumber(),
                "email/refund-processed", ctx, order.getUser(), "REFUND_PROCESSED");
    }

    private void sendEmail(String to, String subject, String template,
                           Context ctx, User user, String event) {
        Notification notification = Notification.builder()
                .user(user)
                .type(NotificationType.EMAIL)
                .event(event)
                .recipient(to)
                .subject(subject)
                .status(NotificationStatus.PENDING)
                .build();

        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
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
                log.info("Email sent to: {} for event: {}", to, event);
                return;
            } catch (Exception e) {
                log.warn("Email attempt {}/{} failed for {}: {}", attempt, maxRetries, to, e.getMessage());
                if (attempt == maxRetries) {
                    notification.setStatus(NotificationStatus.FAILED);
                    notificationRepository.save(notification);
                    log.error("Failed to send email to {} after {} attempts", to, maxRetries);
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
                log.info("[MOCK SMS] To: {} | Message: {}", phone, message);
                notification.setStatus(NotificationStatus.SENT);
            } else {
                // Real Twilio integration would go here
                // Message.creator(new PhoneNumber(phone), new PhoneNumber(fromNumber),  message).create();
                notification.setStatus(NotificationStatus.SENT);
            }
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", phone, e.getMessage());
            notification.setStatus(NotificationStatus.FAILED);
        } finally {
            notificationRepository.save(notification);
        }
    }
}
