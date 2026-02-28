package com.nazir.orderservice.repository;

import com.nazir.orderservice.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    boolean existsByName(String name);
    
    @Query("""
        SELECT c FROM Category c 
        WHERE c.active = true
        AND (:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<Category> findBySearch(@Param("search") String search, Pageable pageable);
}
