package com.nazir.orderservice.dto.response;

import com.nazir.orderservice.enums.Role;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder
public class UserResponse {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private Role role;
    private boolean active;
    private LocalDateTime createdAt;
}
