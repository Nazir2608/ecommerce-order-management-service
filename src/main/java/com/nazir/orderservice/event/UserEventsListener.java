package com.nazir.orderservice.event;

import com.nazir.orderservice.repository.UserRepository;
import com.nazir.orderservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
@RequiredArgsConstructor
public class UserEventsListener {
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        userRepository.findById(event.userId())
                .ifPresent(notificationService::sendWelcomeEmail);
    }
}
