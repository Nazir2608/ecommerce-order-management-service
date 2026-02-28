package com.nazir.orderservice.service;

import com.nazir.orderservice.dto.request.CreateCategoryRequest;
import com.nazir.orderservice.dto.response.CategoryResponse;
import com.nazir.orderservice.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CategoryService {
    CategoryResponse createCategory(CreateCategoryRequest request);
    CategoryResponse updateCategory(UUID id, CreateCategoryRequest request);
    void deleteCategory(UUID id);
    CategoryResponse getCategoryById(UUID id);
    PageResponse<CategoryResponse> getCategories(String search, Pageable pageable);
}
