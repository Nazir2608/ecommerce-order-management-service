package com.nazir.orderservice.controller;

import com.nazir.orderservice.dto.request.AddToCartRequest;
import com.nazir.orderservice.dto.request.UpdateCartItemRequest;
import com.nazir.orderservice.dto.response.CartResponse;
import com.nazir.orderservice.entity.User;
import com.nazir.orderservice.repository.UserRepository;
import com.nazir.orderservice.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(value = "features.cart.enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart management")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Get current user's cart")
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cartService.getCart(getUserId(userDetails)));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<CartResponse> addItem(@AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(cartService.addItem(getUserId(userDetails), request));
    }

    @PutMapping("/items/{cartItemId}")
    @Operation(summary = "Update cart item quantity")
    public ResponseEntity<CartResponse> updateItem(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID cartItemId, @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(cartService.updateItemQuantity(getUserId(userDetails), cartItemId, request));
    }

    @DeleteMapping("/items/{cartItemId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<CartResponse> removeItem(@AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID cartItemId) {
        return ResponseEntity.ok(cartService.removeItem(getUserId(userDetails), cartItemId));
    }

    @DeleteMapping
    @Operation(summary = "Clear entire cart")
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal UserDetails userDetails) {
        cartService.clearCart(getUserId(userDetails));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/coupon")
    @Operation(summary = "Apply coupon code to cart")
    public ResponseEntity<CartResponse> applyCoupon(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String couponCode) {
        return ResponseEntity.ok(cartService.applyCoupon(getUserId(userDetails), couponCode));
    }

    @DeleteMapping("/coupon")
    @Operation(summary = "Remove applied coupon")
    public ResponseEntity<CartResponse> removeCoupon(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cartService.removeCoupon(getUserId(userDetails)));
    }

    private UUID getUserId(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow();
        return user.getId();
    }
}
