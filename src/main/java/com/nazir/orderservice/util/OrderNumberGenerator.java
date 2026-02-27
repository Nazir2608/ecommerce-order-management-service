package com.nazir.orderservice.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

public class OrderNumberGenerator {

    private static final AtomicLong counter = new AtomicLong(0);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Generates a unique order number like: ORD-20240227-000001
     */
    public static String generate() {
        String date = LocalDate.now().format(DATE_FORMAT);
        long seq = counter.incrementAndGet();
        return String.format("ORD-%s-%06d", date, seq);
    }

    private OrderNumberGenerator() {
        // Utility class
    }
}
