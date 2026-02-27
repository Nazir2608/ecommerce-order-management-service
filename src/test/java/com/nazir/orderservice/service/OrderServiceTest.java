package com.nazir.orderservice.service;

import com.nazir.orderservice.enums.OrderStatus;
import com.nazir.orderservice.exception.InvalidOrderStatusTransitionException;
import com.nazir.orderservice.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private com.nazir.orderservice.repository.OrderRepository orderRepository;
    @Mock private com.nazir.orderservice.repository.OrderStatusHistoryRepository orderStatusHistoryRepository;
    @Mock private com.nazir.orderservice.repository.UserRepository userRepository;
    @Mock private com.nazir.orderservice.repository.AddressRepository addressRepository;
    @Mock private com.nazir.orderservice.repository.ProductRepository productRepository;
    @Mock private com.nazir.orderservice.repository.CartItemRepository cartItemRepository;
    @Mock private com.nazir.orderservice.repository.CartRepository cartRepository;
    @Mock private com.nazir.orderservice.repository.PaymentRepository paymentRepository;
    @Mock private CartService cartService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void updateOrderStatus_InvalidTransition_ShouldThrowException() {
        var order = new com.nazir.orderservice.entity.Order();
        order.setStatus(OrderStatus.PENDING);

        org.mockito.Mockito.when(orderRepository.findById(org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.of(order));

        assertThrows(InvalidOrderStatusTransitionException.class,
                () -> orderService.updateOrderStatus(
                        java.util.UUID.randomUUID(),
                        java.util.UUID.randomUUID(),
                        OrderStatus.DELIVERED,
                        "test"));
    }
}
