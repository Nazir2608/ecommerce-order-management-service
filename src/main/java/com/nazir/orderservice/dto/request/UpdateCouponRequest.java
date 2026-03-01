package com.nazir.orderservice.dto.request;

import com.nazir.orderservice.enums.DiscountType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class UpdateCouponRequest {
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private Integer maxUses;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
}