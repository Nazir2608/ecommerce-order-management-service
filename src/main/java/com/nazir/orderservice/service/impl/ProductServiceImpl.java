package com.nazir.orderservice.service.impl;

import com.nazir.orderservice.dto.request.CreateProductRequest;
import com.nazir.orderservice.dto.response.PageResponse;
import com.nazir.orderservice.dto.response.ProductResponse;
import com.nazir.orderservice.entity.Category;
import com.nazir.orderservice.entity.Product;
import com.nazir.orderservice.exception.ResourceNotFoundException;
import com.nazir.orderservice.repository.CategoryRepository;
import com.nazir.orderservice.repository.ProductRepository;
import com.nazir.orderservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Product product = Product.builder()
                .category(category)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .sku(request.getSku())
                .imageUrl(request.getImageUrl())
                .active(true)
                .build();
        product = productRepository.save(product);
        return toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(UUID id, CreateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        product.setCategory(category);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setSku(request.getSku());
        product.setImageUrl(request.getImageUrl());
        product = productRepository.save(product);
        return toResponse(product);
    }

    @Override
    @Transactional
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setActive(false);
        productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id) {
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProducts(UUID categoryId, String search,
                                                     BigDecimal minPrice, BigDecimal maxPrice,
                                                     Pageable pageable) {
        Page<Product> page = productRepository.findByFilters(categoryId, search, minPrice, maxPrice, pageable);
        return PageResponse.of(page.map(this::toResponse));
    }

    @Override
    @Transactional
    public ProductResponse updateStock(UUID id, int quantity) {
        Product product = productRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setStockQuantity(quantity);
        product = productRepository.save(product);
        return toResponse(product);
    }

    private ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .stockQuantity(p.getStockQuantity() == null ? 0 : p.getStockQuantity())
                .sku(p.getSku())
                .imageUrl(p.getImageUrl())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .active(p.isActive())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
