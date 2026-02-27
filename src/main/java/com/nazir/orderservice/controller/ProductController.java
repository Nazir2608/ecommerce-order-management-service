package com.nazir.orderservice.controller;

import com.nazir.orderservice.dto.request.CreateProductRequest;
import com.nazir.orderservice.dto.response.PageResponse;
import com.nazir.orderservice.dto.response.ProductResponse;
import com.nazir.orderservice.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalog APIs")
public class ProductController {

    private final ProductService productService;

    // ===== PUBLIC ENDPOINTS =====

    @GetMapping("/api/v1/products")
    @Operation(summary = "List all active products with filters and pagination")
    public ResponseEntity<PageResponse<ProductResponse>> getProducts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(productService.getProducts(categoryId, search, minPrice, maxPrice, pageable));
    }

    @GetMapping("/api/v1/products/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    // ===== ADMIN ENDPOINTS =====

    @PostMapping("/api/v1/admin/products")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new product", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @PutMapping("/api/v1/admin/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update product", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable UUID id,
            @Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/api/v1/admin/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft-delete product", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/v1/admin/products/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update product stock", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ProductResponse> updateStock(@PathVariable UUID id,
            @RequestParam int quantity) {
        return ResponseEntity.ok(productService.updateStock(id, quantity));
    }
}
