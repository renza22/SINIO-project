package com.sinio.demo.model;

public enum PaymentStatus {
    PENDING, // Payment sedang diproses
    SUCCESS, // Payment berhasil
    FAILED, // Payment gagal
    EXPIRED, // Payment expired (timeout)
    CANCELLED // Payment dibatalkan oleh user
}
