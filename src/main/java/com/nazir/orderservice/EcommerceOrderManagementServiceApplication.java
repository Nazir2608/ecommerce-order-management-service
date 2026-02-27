package com.nazir.orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(scanBasePackages = "com.nazir.orderservice")
@EnableCaching
public class EcommerceOrderManagementServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EcommerceOrderManagementServiceApplication.class, args);
    }
}
