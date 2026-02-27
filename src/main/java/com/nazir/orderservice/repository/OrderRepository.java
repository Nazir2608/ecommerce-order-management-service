package com.nazir.orderservice.repository;

import com.nazir.orderservice.entity.Order;
import com.nazir.orderservice.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Page<Order> findByUserId(UUID userId, Pageable pageable);
    Page<Order> findByUserIdAndStatus(UUID userId, OrderStatus status, Pageable pageable);

    @Query("""
        SELECT o FROM Order o WHERE
        (:status IS NULL OR o.status = :status)
        AND (:from IS NULL OR CAST(o.createdAt AS string) >= :from)
        AND (:to IS NULL OR CAST(o.createdAt AS string) <= :to)
        """)
    Page<Order> findByFilters(@Param("status") OrderStatus status,
        @Param("from") String from, @Param("to") String to, Pageable pageable);
}
