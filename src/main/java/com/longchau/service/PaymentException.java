package com.longchau.service;

/**
 * Lớp ngoại lệ cho các lỗi nghiệp vụ thanh toán
 */
public class PaymentException extends Exception {
    public PaymentException(String message) {
        super(message);
    }
}

