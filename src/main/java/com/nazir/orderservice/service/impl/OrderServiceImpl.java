package com.nazir.orderservice.service.impl;

import com.nazir.orderservice.dto.request.PlaceOrderRequest;
import com.nazir.orderservice.dto.response.OrderItemResponse;
import com.nazir.orderservice.dto.response.OrderResponse;
import com.nazir.orderservice.dto.response.PageResponse;
import com.nazir.orderservice.entity.*;
import com.nazir.orderservice.enums.OrderStatus;
import com.nazir.orderservice.enums.PaymentMethod;
import com.nazir.orderservice.enums.PaymentStatus;
import com.nazir.orderservice.exception.BadRequestException;
import com.nazir.orderservice.exception.InsufficientStockException;
import com.nazir.orderservice.exception.InvalidOrderStatusTransitionException;
import com.nazir.orderservice.exception.ResourceNotFoundException;
import com.nazir.orderservice.repository.*;
import com.nazir.orderservice.service.CartService;
import com.nazir.orderservice.service.NotificationService;
import com.nazir.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final PaymentRepository paymentRepository;
    private final CartService cartService;
    private final NotificationService notificationService;

    // Valid status transitions
    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
            OrderStatus.PENDING, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
            OrderStatus.PROCESSING, Set.of(OrderStatus.SHIPPED),
            OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED, Set.of(OrderStatus.REFUNDED),
            OrderStatus.CANCELLED, Set.of(),
            OrderStatus.REFUNDED, Set.of()
    );

    @Override
    @Transactional
    public OrderResponse placeOrder(UUID userId, PlaceOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Address shippingAddress = addressRepository.findById(request.getShippingAddressId())
                .filter(a -> a.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("Cart is empty"));

        List<CartItem> cartItems = cartItemRepository.findByCart(cart);
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Cannot place order with empty cart");
        }

        // Validate and deduct stock with pessimistic locking
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findByIdForUpdate(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new InsufficientStockException(
                        "Insufficient stock for: " + product.getName() +
                        ". Available: " + product.getStockQuantity());
            }

            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);

            BigDecimal itemTotal = cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            subtotal = subtotal.add(itemTotal);

            orderItems.add(OrderItem.builder()
                    .product(product)
                    .productName(product.getName())
                    .unitPrice(cartItem.getUnitPrice())
                    .quantity(cartItem.getQuantity())
                    .totalPrice(itemTotal)
                    .build());
        }

        BigDecimal shippingAmount = subtotal.compareTo(BigDecimal.valueOf(100)) >= 0
                ? BigDecimal.ZERO : BigDecimal.TEN;
        BigDecimal finalAmount = subtotal.add(shippingAmount);

        Order order = Order.builder()
                .user(user)
                .orderNumber(generateOrderNumber())
                .status(OrderStatus.PENDING)
                .totalAmount(subtotal)
                .discountAmount(BigDecimal.ZERO)
                .shippingAmount(shippingAmount)
                .finalAmount(finalAmount)
                .shippingAddress(shippingAddress)
                .notes(request.getNotes())
                .build();

        order = orderRepository.save(order);

        // Save order items
        for (OrderItem item : orderItems) {
            item.setOrder(order);
        }
        order.setItems(orderItems);
        order = orderRepository.save(order);

        // Save initial status history
        saveStatusHistory(order, null, OrderStatus.PENDING, user, "Order placed");

        // If COD — directly confirm
        if (request.getPaymentMethod() == PaymentMethod.COD) {
            Payment payment = Payment.builder()
                    .order(order)
                    .paymentMethod(PaymentMethod.COD)
                    .paymentStatus(PaymentStatus.PENDING)
                    .amount(finalAmount)
                    .currency("USD")
                    .build();
            paymentRepository.save(payment);
        }

        // Clear cart
        cartItemRepository.deleteAll(cartItems);

        log.info("Order placed: {} for user: {}", order.getOrderNumber(), userId);
        notificationService.sendOrderPlacedNotification(order);

        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID userId, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("Access denied");
        }
        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyOrders(UUID userId, OrderStatus status, Pageable pageable) {
        Page<Order> page = status != null
                ? orderRepository.findByUserIdAndStatus(userId, status, pageable)
                : orderRepository.findByUserId(userId, pageable);
        return PageResponse.of(page.map(this::toResponse));
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("Access denied");
        }

        validateTransition(order.getStatus(), OrderStatus.CANCELLED);

        // Restore stock
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        User user = userRepository.findById(userId).orElseThrow();
        saveStatusHistory(order, oldStatus, OrderStatus.CANCELLED, user, "Cancelled by customer");
        order = orderRepository.save(order);

        notificationService.sendOrderCancelledNotification(order);
        log.info("Order cancelled: {}", order.getOrderNumber());

        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(UUID adminId, OrderStatus status,
            String from, String to, Pageable pageable) {
        Page<Order> page = orderRepository.findByFilters(status, from, to, pageable);
        return PageResponse.of(page.map(this::toResponse));
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(UUID adminId, UUID orderId, OrderStatus newStatus, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        validateTransition(order.getStatus(), newStatus);

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);

        User admin = userRepository.findById(adminId).orElseThrow();
        saveStatusHistory(order, oldStatus, newStatus, admin, reason);

        order = orderRepository.save(order);
        notificationService.sendOrderStatusUpdatedNotification(order);

        log.info("Order {} status updated: {} -> {}", order.getOrderNumber(), oldStatus, newStatus);
        return toResponse(order);
    }

    private void validateTransition(OrderStatus current, OrderStatus next) {
        Set<OrderStatus> allowed = VALID_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(next)) {
            throw new InvalidOrderStatusTransitionException(
                    "Cannot transition from " + current + " to " + next);
        }
    }

    private void saveStatusHistory(Order order, OrderStatus oldStatus, OrderStatus newStatus,
            User changedBy, String reason) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .oldStatus(oldStatus != null ? oldStatus.name() : null)
                .newStatus(newStatus.name())
                .changedBy(changedBy)
                .reason(reason)
                .build();
        orderStatusHistoryRepository.save(history);
    }

    private String generateOrderNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = orderRepository.count() + 1;
        return String.format("ORD-%s-%04d", date, count);
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems() == null ? List.of() :
                order.getItems().stream().map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProductName())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .totalPrice(item.getTotalPrice())
                        .build()).collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .shippingAmount(order.getShippingAmount())
                .finalAmount(order.getFinalAmount())
                .notes(order.getNotes())
                .items(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
