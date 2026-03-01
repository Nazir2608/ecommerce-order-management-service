package com.nazir.orderservice.service.impl;

import com.nazir.orderservice.config.AppProperties;
import com.nazir.orderservice.dto.request.AddToCartRequest;
import com.nazir.orderservice.dto.request.UpdateCartItemRequest;
import com.nazir.orderservice.dto.response.CartItemResponse;
import com.nazir.orderservice.dto.response.CartResponse;
import com.nazir.orderservice.entity.*;
import com.nazir.orderservice.exception.InsufficientStockException;
import com.nazir.orderservice.exception.ResourceNotFoundException;
import com.nazir.orderservice.repository.*;
import com.nazir.orderservice.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository     cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository  productRepository;
    private final UserRepository     userRepository;
    private final CouponRepository   couponRepository;
    private final AppProperties      appProperties;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(UUID userId) {
        User user = getUser(userId);
        Cart cart = getOrCreateCart(user);
        return toResponse(cart);
    }


    @Override
    @Transactional
    public CartResponse addItem(UUID userId, AddToCartRequest request) {
        User user = getUser(userId);
        Cart cart = getOrCreateCart(user);
        // Validate product
        Product product = productRepository.findByIdAndActiveTrue(request.getProductId()).orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        // Validate stock
        if (product.getStockQuantity() < request.getQuantity()) {
            throw new InsufficientStockException(product.getName(), request.getQuantity(), product.getStockQuantity());
        }
        // If item already in cart → increment quantity
        Optional<CartItem> existing = cart.getItems().stream().filter(i -> i.getProduct().getId().equals(request.getProductId())).findFirst();

        if (existing.isPresent()) {
            CartItem item = existing.get();
            int newQty = item.getQuantity() + request.getQuantity();
            // Re-validate stock for new total quantity
            if (product.getStockQuantity() < newQty) {
                throw new InsufficientStockException(product.getName(), newQty, product.getStockQuantity());
            }
            item.setQuantity(newQty);
            item.setUnitPrice(product.getPrice()); // refresh price
            cartItemRepository.save(item);
            log.info("Cart item quantity updated: product={}, qty={}", product.getName(), newQty);
        } else {
            // Add new item
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();
            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
            log.info("Item added to cart: product={}, qty={}", product.getName(), request.getQuantity());
        }

        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateItemQuantity(UUID userId, UUID cartItemId, UpdateCartItemRequest request) {
        Cart cart = getCartByUserId(userId);
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        int quantity = request.quantity();

        if (quantity <= 0) {
            // Remove item if quantity is 0 or negative
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
            log.info("Cart item removed (qty=0): itemId={}", cartItemId);
        } else {
            // Validate stock
            Product product = item.getProduct();
            if (product.getStockQuantity() < quantity) {
                throw new InsufficientStockException(product.getName(), quantity, product.getStockQuantity());
            }
            item.setQuantity(quantity);
            cartItemRepository.save(item);
            log.info("Cart item quantity set: itemId={}, qty={}", cartItemId, quantity);
        }

        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeItem(UUID userId, UUID cartItemId) {
        Cart cart = getCartByUserId(userId);
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        cart.getItems().remove(item);
        cartItemRepository.delete(item);
        log.info("Cart item removed: itemId={}", cartItemId);

        return toResponse(cart);
    }

    @Override
    @Transactional
    public void clearCart(UUID userId) {
        Cart cart = getCartByUserId(userId);
        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();
        cart.setCouponCode(null);
        cartRepository.save(cart);
        log.info("Cart cleared for userId={}", userId);
    }


    @Override
    @Transactional
    public CartResponse applyCoupon(UUID userId, String couponCode) {
        Cart cart = getCartByUserId(userId);
        // Validate coupon exists and is active
        Coupon coupon = couponRepository.findByCodeAndActiveTrue(couponCode).orElseThrow(() -> new ResourceNotFoundException("Coupon not found or expired: " + couponCode));
        // Validate expiry
        if (coupon.getValidUntil() != null && coupon.getValidUntil().isBefore(java.time.LocalDateTime.now())) {
            throw new ResourceNotFoundException("Coupon has expired: " + couponCode);
        }
        // Validate usage limit
        if (coupon.getMaxUses() != null && coupon.getUsedCount() >= coupon.getMaxUses()) {
            throw new ResourceNotFoundException("Coupon usage limit reached: " + couponCode);
        }
        // Validate minimum order value
        BigDecimal subtotal = calculateSubtotal(cart);
        if (coupon.getMinOrderValue() != null && subtotal.compareTo(coupon.getMinOrderValue()) < 0) {
            throw new IllegalArgumentException("Minimum order value for this coupon is: " + coupon.getMinOrderValue());
        }
        cart.setCouponCode(couponCode);
        cartRepository.save(cart);
        log.info("Coupon applied: {} for userId={}", couponCode, userId);

        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeCoupon(UUID userId) {
        Cart cart = getCartByUserId(userId);
        cart.setCouponCode(null);
        cartRepository.save(cart);
        log.info("Coupon removed for userId={}", userId);
        return toResponse(cart);
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /** Gets existing cart or creates a new one for the user */
    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .user(user)
                            .build();
                    Cart saved = cartRepository.save(newCart);
                    log.info("New cart created for userId={}", user.getId());
                    return saved;
                });
    }

    private Cart getCartByUserId(UUID userId) {
        User user = getUser(userId);
        return cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user"));
    }

    private BigDecimal calculateSubtotal(Cart cart) {
        return cart.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateDiscount(Cart cart, BigDecimal subtotal) {
        if (cart.getCouponCode() == null) return BigDecimal.ZERO;

        return couponRepository.findByCodeAndActiveTrue(cart.getCouponCode())
                .map(coupon -> {
                    switch (coupon.getDiscountType()) {
                        case PERCENTAGE:
                            // e.g. 10% off
                            return subtotal.multiply(coupon.getDiscountValue())
                                    .divide(BigDecimal.valueOf(100));
                        case FLAT:
                            // e.g. $20 flat off — don't go below 0
                            return coupon.getDiscountValue().min(subtotal);
                        default:
                            return BigDecimal.ZERO;
                    }
                })
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal calculateShipping(BigDecimal subtotal, BigDecimal discount) {
        BigDecimal afterDiscount = subtotal.subtract(discount);
        BigDecimal threshold = appProperties.getOrder().getFreeShippingThreshold();
        BigDecimal charge    = appProperties.getOrder().getShippingCharge();

        // Free shipping above threshold
        return afterDiscount.compareTo(threshold) >= 0 ? BigDecimal.ZERO : charge;
    }

    /** Maps Cart entity → CartResponse DTO with all calculated amounts */
    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        BigDecimal subtotal  = calculateSubtotal(cart);
        BigDecimal discount  = calculateDiscount(cart, subtotal);
        BigDecimal shipping  = calculateShipping(subtotal, discount);
        BigDecimal total     = subtotal.subtract(discount).add(shipping);

        return CartResponse.builder()
                .cartId(cart.getId())
                .items(itemResponses)
                .itemCount(itemResponses.stream().mapToInt(CartItemResponse::getQuantity).sum())
                .subtotal(subtotal)
                .discountAmount(discount)
                .shippingAmount(shipping)
                .totalAmount(total)
                .appliedCoupon(cart.getCouponCode())
                .build();
    }

    private CartItemResponse toItemResponse(CartItem item) {
        return CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productImageUrl(item.getProduct().getImageUrl())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .build();
    }
}