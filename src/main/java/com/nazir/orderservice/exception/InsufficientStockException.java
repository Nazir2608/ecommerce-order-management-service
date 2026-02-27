package com.nazir.orderservice.exception;
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) { super(message); }
}
