package com.nazir.orderservice.dto.response;

import com.nazir.orderservice.enums.PaymentMethod;
import com.nazir.orderservice.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder
public class PaymentResponse {
    private UUID id;
    private UUID orderId;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private BigDecimal amount;
    private String currency;
    private String transactionId;
    private String clientSecret;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
