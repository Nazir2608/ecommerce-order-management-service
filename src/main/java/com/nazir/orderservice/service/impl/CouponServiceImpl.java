package com.nazir.orderservice.service.impl;

import com.nazir.orderservice.dto.request.CreateCouponRequest;
import com.nazir.orderservice.dto.request.UpdateCouponRequest;
import com.nazir.orderservice.dto.response.CouponResponse;
import com.nazir.orderservice.entity.Coupon;
import com.nazir.orderservice.exception.ResourceNotFoundException;
import com.nazir.orderservice.repository.CouponRepository;
import com.nazir.orderservice.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;

    @Override
    @Transactional
    public CouponResponse createCoupon(CreateCouponRequest request) {
        if (couponRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Coupon code already exists");
        }

        Coupon coupon = Coupon.builder()
                .code(request.getCode())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minOrderValue(request.getMinOrderValue())
                .maxUses(request.getMaxUses())
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .active(true)
                .build();

        return toResponse(couponRepository.save(coupon));
    }
    @Override
    @Transactional
    public CouponResponse updateCoupon(UUID id, UpdateCouponRequest request) {
        Coupon coupon = couponRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
        coupon.setDiscountType(request.getDiscountType() != null ? request.getDiscountType() : coupon.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue() != null ? request.getDiscountValue() : coupon.getDiscountValue());
        coupon.setMinOrderValue(request.getMinOrderValue() != null ? request.getMinOrderValue() : coupon.getMinOrderValue());
        coupon.setMaxUses(request.getMaxUses() != null ? request.getMaxUses() : coupon.getMaxUses());
        coupon.setValidFrom(request.getValidFrom() != null ? request.getValidFrom() : coupon.getValidFrom());
        coupon.setValidUntil(request.getValidUntil() != null ? request.getValidUntil() : coupon.getValidUntil());

        return toResponse(coupon);
    }

    @Override
    @Transactional
    public void deactivateCoupon(UUID id) {
        Coupon coupon = couponRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
        coupon.setActive(false);
        couponRepository.save(coupon);
    }

    @Override
    public CouponResponse getById(UUID id) {
        return couponRepository.findById(id).map(this::toResponse).orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
    }

    @Override
    public List<CouponResponse> getAll() {
        return couponRepository.findAll().stream().map(this::toResponse).toList();
    }

    private CouponResponse toResponse(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minOrderValue(coupon.getMinOrderValue())
                .maxUses(coupon.getMaxUses())
                .usedCount(coupon.getUsedCount())
                .validFrom(coupon.getValidFrom())
                .validUntil(coupon.getValidUntil())
                .active(coupon.getActive())
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .build();
    }
}