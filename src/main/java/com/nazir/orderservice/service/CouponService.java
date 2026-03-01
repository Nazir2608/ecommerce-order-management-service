package com.nazir.orderservice.service;

import com.nazir.orderservice.dto.request.CreateCouponRequest;
import com.nazir.orderservice.dto.request.UpdateCouponRequest;
import com.nazir.orderservice.dto.response.CouponResponse;

import java.util.List;
import java.util.UUID;

public interface CouponService {

    CouponResponse createCoupon(CreateCouponRequest request);
    CouponResponse updateCoupon(UUID id, UpdateCouponRequest request);
    void deactivateCoupon(UUID id);
    CouponResponse getById(UUID id);
    List<CouponResponse> getAll();
}