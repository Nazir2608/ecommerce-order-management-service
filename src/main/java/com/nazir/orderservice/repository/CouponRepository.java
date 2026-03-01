package com.nazir.orderservice.repository;

import com.nazir.orderservice.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {
    Optional<Coupon> findByCodeAndActiveTrue(String code);
    boolean existsByCode(String code);
}
