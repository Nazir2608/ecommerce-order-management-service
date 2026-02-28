package com.nazir.orderservice.controller;

import com.nazir.orderservice.dto.request.CreateCategoryRequest;
import com.nazir.orderservice.dto.response.CategoryResponse;
import com.nazir.orderservice.dto.response.PageResponse;
import com.nazir.orderservice.service.CategoryService;
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

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Product categories APIs")
public class CategoryController {

    private final CategoryService categoryService;

    // ===== PUBLIC ENDPOINTS =====

    @GetMapping("/api/v1/categories")
    @Operation(summary = "List categories with optional search and pagination")
    public ResponseEntity<PageResponse<CategoryResponse>> getCategories(@RequestParam(required = false) String search, @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(categoryService.getCategories(search, pageable));
    }

    @GetMapping("/api/v1/categories/{id}")
    @Operation(summary = "Get category by ID")
    public ResponseEntity<CategoryResponse> getCategory(@PathVariable UUID id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    // ===== ADMIN ENDPOINTS =====

    @PostMapping("/api/v1/admin/categories")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new category", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(request));
    }

    @PutMapping("/api/v1/admin/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update category", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable UUID id, @Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @DeleteMapping("/api/v1/admin/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft delete category", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
