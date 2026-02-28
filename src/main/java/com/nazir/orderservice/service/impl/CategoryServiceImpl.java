package com.nazir.orderservice.service.impl;

import com.nazir.orderservice.dto.request.CreateCategoryRequest;
import com.nazir.orderservice.dto.response.CategoryResponse;
import com.nazir.orderservice.dto.response.PageResponse;
import com.nazir.orderservice.entity.Category;
import com.nazir.orderservice.exception.DuplicateResourceException;
import com.nazir.orderservice.exception.ResourceNotFoundException;
import com.nazir.orderservice.repository.CategoryRepository;
import com.nazir.orderservice.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Category already exists with name: " + request.getName());
        }
        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .active(true)
                .build();
        category = categoryRepository.save(category);
        return toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(UUID id, CreateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (!category.getName().equalsIgnoreCase(request.getName())
                && categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Category already exists with name: " + request.getName());
        }
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category = categoryRepository.save(category);
        return toResponse(category);
    }

    @Override
    @Transactional
    public void deleteCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        category.setActive(false);
        categoryRepository.save(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        return toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getCategories(String search, Pageable pageable) {
        Page<Category> page = categoryRepository.findBySearch(
                (search == null || search.isBlank()) ? null : search, pageable);
        return PageResponse.of(page.map(this::toResponse));
    }

    private CategoryResponse toResponse(Category c) {
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .active(c.isActive())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
