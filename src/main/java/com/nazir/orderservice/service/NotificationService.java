package com.nazir.orderservice.service;

import com.nazir.orderservice.entity.Order;
import com.nazir.orderservice.entity.User;

public interface NotificationService {

    void sendWelcomeEmail(User user);

    void sendOrderPlacedNotification(Order order);

    void sendPaymentSuccessNotification(Order order);

    void sendPaymentFailedNotification(Order order);

    void sendOrderStatusUpdatedNotification(Order order);

    void sendOrderCancelledNotification(Order order);

    void sendRefundProcessedNotification(Order order);
}
