package com.nazir.orderservice.dto.response;

import com.nazir.orderservice.enums.DiscountType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CouponResponse {

    private UUID id;

    private String code;

    private DiscountType discountType;

    private BigDecimal discountValue;

    private BigDecimal minOrderValue;

    private Integer maxUses;

    private Integer usedCount;

    private LocalDateTime validFrom;

    private LocalDateTime validUntil;

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}