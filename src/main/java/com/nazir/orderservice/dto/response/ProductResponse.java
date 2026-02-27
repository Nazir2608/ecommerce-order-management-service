package com.nazir.orderservice.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder
public class ProductResponse {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private int stockQuantity;
    private String sku;
    private String imageUrl;
    private UUID categoryId;
    private String categoryName;
    private boolean active;
    private LocalDateTime createdAt;
}
