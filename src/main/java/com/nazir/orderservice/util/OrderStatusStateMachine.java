package com.nazir.orderservice.util;

import com.nazir.orderservice.enums.OrderStatus;
import com.nazir.orderservice.exception.InvalidOrderStatusTransitionException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class OrderStatusStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(OrderStatus.PENDING,
                EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));

        ALLOWED_TRANSITIONS.put(OrderStatus.CONFIRMED,
                EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED));

        ALLOWED_TRANSITIONS.put(OrderStatus.PROCESSING,
                EnumSet.of(OrderStatus.SHIPPED));

        ALLOWED_TRANSITIONS.put(OrderStatus.SHIPPED,
                EnumSet.of(OrderStatus.DELIVERED));

        ALLOWED_TRANSITIONS.put(OrderStatus.DELIVERED,
                EnumSet.of(OrderStatus.REFUNDED));

        ALLOWED_TRANSITIONS.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED_TRANSITIONS.put(OrderStatus.REFUNDED, EnumSet.noneOf(OrderStatus.class));
    }

    public static void validateTransition(OrderStatus from, OrderStatus to) {
        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, EnumSet.noneOf(OrderStatus.class));
        if (!allowed.contains(to)) {
            throw new InvalidOrderStatusTransitionException("Cannot transition from " + from + " to " + to);
        }
    }

    public static boolean isTerminal(OrderStatus status) {
        return status == OrderStatus.CANCELLED || status == OrderStatus.REFUNDED;
    }

    private OrderStatusStateMachine() {
        // Utility class
    }
}
