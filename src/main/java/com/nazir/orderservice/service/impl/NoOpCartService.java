//package com.nazir.orderservice.service.impl;
//
//import com.nazir.orderservice.dto.request.AddToCartRequest;
//import com.nazir.orderservice.dto.response.CartResponse;
//import com.nazir.orderservice.service.CartService;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
//import org.springframework.stereotype.Service;
//
//import java.math.BigDecimal;
//import java.util.List;
//import java.util.UUID;
//
//@Service
//public class NoOpCartService implements CartService {
//
//    private CartResponse empty(UUID userId) {
//        return CartResponse.builder()
//                .cartId(userId)
//                .items(List.of())
//                .itemCount(0)
//                .subtotal(BigDecimal.ZERO)
//                .discountAmount(BigDecimal.ZERO)
//                .shippingAmount(BigDecimal.ZERO)
//                .totalAmount(BigDecimal.ZERO)
//                .appliedCoupon(null)
//                .build();
//    }
//
//    @Override
//    public CartResponse getCart(UUID userId) {
//        return empty(userId);
//    }
//
//    @Override
//    public CartResponse addItem(UUID userId, AddToCartRequest request) {
//        return empty(userId);
//    }
//
//    @Override
//    public CartResponse updateItemQuantity(UUID userId, UUID cartItemId, int quantity) {
//        return empty(userId);
//    }
//
//    @Override
//    public CartResponse removeItem(UUID userId, UUID cartItemId) {
//        return empty(userId);
//    }
//
//    @Override
//    public void clearCart(UUID userId) {
//        // no-op
//    }
//
//    @Override
//    public CartResponse applyCoupon(UUID userId, String couponCode) {
//        return empty(userId);
//    }
//
//    @Override
//    public CartResponse removeCoupon(UUID userId) {
//        return empty(userId);
//    }
//}
