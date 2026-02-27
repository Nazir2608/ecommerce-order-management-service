package com.nazir.orderservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Category extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String name;

    private String description;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;
}
