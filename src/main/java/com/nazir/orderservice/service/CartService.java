package com.nazir.orderservice.service;

import com.nazir.orderservice.dto.request.AddToCartRequest;
import com.nazir.orderservice.dto.request.UpdateCartItemRequest;
import com.nazir.orderservice.dto.response.CartResponse;

import java.util.UUID;

public interface CartService {

    CartResponse getCart(UUID userId);

    CartResponse addItem(UUID userId, AddToCartRequest request);

    CartResponse updateItemQuantity(UUID userId, UUID cartItemId, UpdateCartItemRequest request);

    CartResponse removeItem(UUID userId, UUID cartItemId);

    void clearCart(UUID userId);

    CartResponse applyCoupon(UUID userId, String couponCode);

    CartResponse removeCoupon(UUID userId);
}
