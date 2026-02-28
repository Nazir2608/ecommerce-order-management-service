package com.nazir.orderservice.specification;

import com.nazir.orderservice.entity.Product;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.UUID;

public class ProductSpecification {

    public static Specification<Product> build(UUID categoryId, String search, BigDecimal minPrice, BigDecimal maxPrice) {
        return Specification.where(isActive()).and(hasCategory(categoryId)).and(nameContains(search)).and(priceGte(minPrice)).and(priceLte(maxPrice));
    }

    private static Specification<Product> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    private static Specification<Product> hasCategory(UUID categoryId) {
        if (categoryId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    private static Specification<Product> nameContains(String search) {
        if (search == null || search.isBlank()) return null;
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%");
    }

    private static Specification<Product> priceGte(BigDecimal min) {
        if (min == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), min);
    }

    private static Specification<Product> priceLte(BigDecimal max) {
        if (max == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), max);
    }
}