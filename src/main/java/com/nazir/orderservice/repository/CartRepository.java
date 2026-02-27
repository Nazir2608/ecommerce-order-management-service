package com.nazir.orderservice.repository;

import com.nazir.orderservice.entity.Cart;
import com.nazir.orderservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {
    Optional<Cart> findByUser(User user);
}
