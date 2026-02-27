package com.nazir.orderservice.service;

import com.nazir.orderservice.dto.request.PlaceOrderRequest;
import com.nazir.orderservice.dto.response.OrderResponse;
import com.nazir.orderservice.dto.response.PageResponse;
import com.nazir.orderservice.enums.OrderStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {

    OrderResponse placeOrder(UUID userId, PlaceOrderRequest request);

    OrderResponse getOrderById(UUID userId, UUID orderId);

    PageResponse<OrderResponse> getMyOrders(UUID userId, OrderStatus status, Pageable pageable);

    OrderResponse cancelOrder(UUID userId, UUID orderId);

    // Admin operations
    PageResponse<OrderResponse> getAllOrders(UUID adminId, OrderStatus status,
                                             String from, String to,
                                             Pageable pageable);

    OrderResponse updateOrderStatus(UUID adminId, UUID orderId, OrderStatus newStatus, String reason);
}
