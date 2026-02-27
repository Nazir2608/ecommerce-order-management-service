package com.nazir.orderservice.dto.request;

import com.nazir.orderservice.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateOrderStatusRequest {

    @NotNull(message = "New status is required")
    private OrderStatus newStatus;

    private String reason;
}
