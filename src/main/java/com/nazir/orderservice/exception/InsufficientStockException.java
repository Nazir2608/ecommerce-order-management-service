package com.nazir.orderservice.exception;
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String productName, int requestedQuantity, int availableStock) {
        super(String.format("Insufficient stock for product '%s'. Requested: %d, Available: %d", productName, requestedQuantity, availableStock
        ));
    }
}