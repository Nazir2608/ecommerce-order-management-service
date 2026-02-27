package com.nazir.orderservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateProductRequest {
    @NotNull
    private UUID categoryId;
    @NotBlank @Size(max = 200)
    private String name;
    private String description;
    @NotNull @DecimalMin("0.01")
    private BigDecimal price;
    @Min(0)
    private int stockQuantity;
    @NotBlank @Size(max = 100)
    private String sku;
    private String imageUrl;
}
