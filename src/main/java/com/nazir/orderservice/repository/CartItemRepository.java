package com.nazir.orderservice.repository;

import com.nazir.orderservice.entity.Cart;
import com.nazir.orderservice.entity.CartItem;
import com.nazir.orderservice.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    List<CartItem> findByCart(Cart cart);
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
    void deleteAll(Iterable<? extends CartItem> items);
}
