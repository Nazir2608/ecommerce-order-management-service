package com.nazir.orderservice.repository;

import com.nazir.orderservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
}
