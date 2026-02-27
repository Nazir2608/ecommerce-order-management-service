package com.nazir.orderservice.service;

import com.nazir.orderservice.dto.request.CreateProductRequest;
import com.nazir.orderservice.dto.response.PageResponse;
import com.nazir.orderservice.dto.response.ProductResponse;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public interface ProductService {

    ProductResponse createProduct(CreateProductRequest request);

    ProductResponse updateProduct(UUID id, CreateProductRequest request);

    void deleteProduct(UUID id);

    ProductResponse getProductById(UUID id);

    PageResponse<ProductResponse> getProducts(UUID categoryId, String search,
                                              BigDecimal minPrice, BigDecimal maxPrice,
                                              Pageable pageable);

    ProductResponse updateStock(UUID id, int quantity);
}
