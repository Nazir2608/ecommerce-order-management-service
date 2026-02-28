package com.nazir.orderservice.config;
import com.nazir.orderservice.entity.User;
import com.nazir.orderservice.enums.Role;
import com.nazir.orderservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String ADMIN_EMAIL    = "admin@orderservice.com";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String ADMIN_NAME     = "Super Admin";

    @Override
    public void run(ApplicationArguments args) {
        seedAdminUser();
    }

    private void seedAdminUser() {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            log.info("Admin user already exists — skipping seed.");
            return;
        }
        User admin = User.builder().name(ADMIN_NAME)
                .email(ADMIN_EMAIL)
                .password(passwordEncoder.encode(ADMIN_PASSWORD)) // hashed at runtime
                .role(Role.ADMIN)
                .isActive(true)
                .build();

        userRepository.save(admin);
        log.info("  Admin user seeded successfully");
        log.info("  Email   : {}", ADMIN_EMAIL);
        log.info("  Role    : ADMIN");
    }
}