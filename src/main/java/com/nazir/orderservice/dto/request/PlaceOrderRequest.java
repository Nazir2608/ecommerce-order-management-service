package com.nazir.orderservice.dto.request;

import com.nazir.orderservice.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class PlaceOrderRequest {
    @NotNull
    private UUID shippingAddressId;
    @NotNull
    private PaymentMethod paymentMethod;
    private String couponCode;
    private String notes;
}
