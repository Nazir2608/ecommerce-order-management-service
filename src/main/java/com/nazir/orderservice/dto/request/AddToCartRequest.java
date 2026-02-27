package com.nazir.orderservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.UUID;

@Data
public class AddToCartRequest {
    @NotNull
    private UUID productId;
    @Min(1) @Max(100)
    private int quantity;
}
