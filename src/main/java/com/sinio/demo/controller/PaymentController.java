package com.sinio.demo.controller;

import com.sinio.demo.model.Payment;
import com.sinio.demo.model.PaymentStatus;
import com.sinio.demo.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    /**
     * Membuat payment transaction dan mendapatkan Snap token
     * Endpoint: POST /api/payment/create/{reservationId}
     */
    @PostMapping("/create/{reservationId}")
    public ResponseEntity<?> createPayment(@PathVariable Long reservationId,
                                           @RequestParam(name = "method", defaultValue = "midtrans") String method) {
        try {
            Payment payment = paymentService.createTransaction(reservationId, method);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orderId", payment.getOrderId());
            response.put("clientKey", paymentService.getClientKey());
            response.put("paymentType", payment.getPaymentType());

            if (payment.getSnapToken() != null) {
                response.put("snapToken", payment.getSnapToken());
            }
            if ("cash".equalsIgnoreCase(method)) {
                response.put("message", "Reservasi dicatat dengan metode bayar cash di hotel.");
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Endpoint untuk menerima notifikasi dari Midtrans (webhook)
     * Endpoint: POST /api/payment/notification
     */
    @PostMapping("/notification")
    public ResponseEntity<?> handleNotification(@RequestBody Map<String, Object> notification) {
        try {
            paymentService.handleNotification(notification);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Check payment status
     * Endpoint: GET /api/payment/status/{orderId}
     */
    @GetMapping("/status/{orderId}")
    public ResponseEntity<?> checkStatus(@PathVariable String orderId) {
        try {
            Payment payment = paymentService.getPaymentByOrderId(orderId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("status", payment.getStatus().toString());
            response.put("amount", payment.getAmount());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
}
