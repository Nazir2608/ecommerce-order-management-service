package com.nazir.orderservice.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data @Builder
public class OrderItemResponse {
    private UUID id;
    private UUID productId;
    private String productName;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal totalPrice;
}
