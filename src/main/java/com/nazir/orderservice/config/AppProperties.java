package com.nazir.orderservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Stripe stripe = new Stripe();
    private Twilio twilio = new Twilio();
    private Order order = new Order();
    private RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long accessTokenExpiryMs;
        private long refreshTokenExpiryMs;
    }

    @Getter
    @Setter
    public static class Stripe {
        private String secretKey;
        private String webhookSecret;
    }

    @Getter
    @Setter
    public static class Twilio {
        private String accountSid;
        private String authToken;
        private String phoneNumber;
        private boolean enabled;
    }

    @Getter
    @Setter
    public static class Order {
        private BigDecimal freeShippingThreshold = new BigDecimal("100.00");
        private BigDecimal shippingCharge = new BigDecimal("10.00");
        private int lowStockThreshold = 10;
    }

    @Getter
    @Setter
    public static class RateLimit {
        private int requestsPerMinute = 100;
    }
}
