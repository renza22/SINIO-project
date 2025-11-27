package com.sinio.demo.service;

import com.midtrans.Config;
import com.midtrans.ConfigFactory;
import com.midtrans.httpclient.error.MidtransError;
import com.midtrans.service.MidtransSnapApi;
import com.sinio.demo.model.Payment;
import com.sinio.demo.model.PaymentStatus;
import com.sinio.demo.model.Reservation;
import com.sinio.demo.model.Room;
import com.sinio.demo.repository.PaymentRepository;
import com.sinio.demo.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Value("${midtrans.server.key}")
    private String serverKey;

    @Value("${midtrans.client.key}")
    private String clientKey;

    @Value("${midtrans.is.production}")
    private boolean isProduction;

    /**
     * Calculate total amount untuk reservasi
     */
    public BigDecimal calculateTotalAmount(Reservation reservation) {
        Room room = reservation.getRoom();
        LocalDate checkIn = reservation.getCheckIn();
        LocalDate checkOut = reservation.getCheckOut();

        // Hitung jumlah malam
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights <= 0) {
            nights = 1; // Minimal 1 malam
        }

        // Total = (harga kamar per malam * jumlah malam) + layanan tambahan
        BigDecimal roomTotal = room.getRate().multiply(BigDecimal.valueOf(nights));
        BigDecimal servicesTotal = reservation.getRequestedServicesTotal();

        return roomTotal.add(servicesTotal);
    }

    /**
     * Create payment transaction dan generate Snap token
     */
    @Transactional
    public Payment createTransaction(Long reservationId, String paymentMethod) throws MidtransError {
        // Get reservation
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        String method = paymentMethod != null ? paymentMethod.trim().toLowerCase() : "midtrans";

        // Check if payment already exists for this reservation
        List<Payment> existingPayments = paymentRepository.findByReservationId(reservationId);
        for (Payment p : existingPayments) {
            if (p.getStatus() == PaymentStatus.PENDING || p.getStatus() == PaymentStatus.SUCCESS) {
                // Return existing pending/success payment
                return p;
            }
        }

        // Calculate total amount
        BigDecimal totalAmount = calculateTotalAmount(reservation);

        // Generate unique order ID
        String orderId = generateOrderId(reservation);

        // Create payment record
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setReservation(reservation);
        payment.setAmount(totalAmount);

        // Cash: skip Midtrans, simpan sebagai pending cash
        if ("cash".equals(method)) {
            payment.setStatus(PaymentStatus.PENDING);
            payment.setPaymentType("CASH");
            payment.setTransactionTime(LocalDateTime.now());
            return paymentRepository.save(payment);
        }

        // Online via Midtrans
        payment.setStatus(PaymentStatus.PENDING);

        // Build Midtrans transaction data
        Map<String, Object> transactionDetails = buildTransactionDetails(orderId, totalAmount);
        Map<String, Object> customerDetails = buildCustomerDetails(reservation);
        List<Map<String, Object>> itemDetails = buildItemDetails(reservation, totalAmount);

        Map<String, Object> params = new HashMap<>();
        params.put("transaction_details", transactionDetails);
        params.put("customer_details", customerDetails);
        params.put("item_details", itemDetails);

        Config midtransConfig = new Config(serverKey, clientKey, isProduction);
        MidtransSnapApi snapApi = new ConfigFactory(midtransConfig).getSnapApi();
        String snapToken = snapApi.createTransactionToken(params);

        payment.setSnapToken(snapToken);
        payment.setPaymentType("MIDTRANS");
        payment.setTransactionTime(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    /**
     * Generate unique order ID
     */
    private String generateOrderId(Reservation reservation) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "ORD-" + reservation.getId() + "-" + timestamp;
    }

    /**
     * Build transaction details for Midtrans
     */
    private Map<String, Object> buildTransactionDetails(String orderId, BigDecimal amount) {
        Map<String, Object> details = new HashMap<>();
        details.put("order_id", orderId);
        details.put("gross_amount", amount.longValue());
        return details;
    }

    /**
     * Build customer details for Midtrans
     */
    private Map<String, Object> buildCustomerDetails(Reservation reservation) {
        Map<String, Object> customer = new HashMap<>();
        customer.put("first_name", reservation.getUser().getFullName());
        customer.put("email", reservation.getUser().getEmail());
        customer.put("phone", "081234567890"); // Default phone since User model doesn't have phone field
        return customer;
    }

    /**
     * Build item details for Midtrans
     */
    private List<Map<String, Object>> buildItemDetails(Reservation reservation, BigDecimal totalAmount) {
        List<Map<String, Object>> items = new ArrayList<>();

        Room room = reservation.getRoom();
        LocalDate checkIn = reservation.getCheckIn();
        LocalDate checkOut = reservation.getCheckOut();
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights <= 0)
            nights = 1;

        // Room item
        Map<String, Object> roomItem = new HashMap<>();
        roomItem.put("id", "ROOM-" + room.getId());
        roomItem.put("name", room.getNumber() + " - " + room.getType().getDisplayName() + " (" + nights + " malam)");
        roomItem.put("price", room.getRate().longValue());
        roomItem.put("quantity", (int) nights);
        items.add(roomItem);

        // Service items
        BigDecimal servicesTotal = reservation.getRequestedServicesTotal();
        if (servicesTotal.compareTo(BigDecimal.ZERO) > 0) {
            Map<String, Object> serviceItem = new HashMap<>();
            serviceItem.put("id", "SERVICES");
            serviceItem.put("name", "Layanan Tambahan");
            serviceItem.put("price", servicesTotal.longValue());
            serviceItem.put("quantity", 1);
            items.add(serviceItem);
        }

        return items;
    }

    /**
     * Handle notification dari Midtrans (webhook/callback)
     */
    @Transactional
    public void handleNotification(Map<String, Object> notification) {
        try {
            String orderId = (String) notification.get("order_id");
            String transactionStatus = (String) notification.get("transaction_status");
            String fraudStatus = (String) notification.get("fraud_status");
            String transactionId = (String) notification.get("transaction_id");
            String paymentType = (String) notification.get("payment_type");

            Payment payment = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderId));

            // Update transaction details
            payment.setTransactionId(transactionId);
            payment.setPaymentType(paymentType);
            payment.setTransactionTime(LocalDateTime.now());

            // Update status based on transaction status
            if (transactionStatus.equals("capture")) {
                if (fraudStatus.equals("accept")) {
                    payment.setStatus(PaymentStatus.SUCCESS);
                }
            } else if (transactionStatus.equals("settlement")) {
                payment.setStatus(PaymentStatus.SUCCESS);
            } else if (transactionStatus.equals("pending")) {
                payment.setStatus(PaymentStatus.PENDING);
            } else if (transactionStatus.equals("deny") || transactionStatus.equals("cancel")) {
                payment.setStatus(PaymentStatus.CANCELLED);
            } else if (transactionStatus.equals("expire")) {
                payment.setStatus(PaymentStatus.EXPIRED);
            } else if (transactionStatus.equals("failure")) {
                payment.setStatus(PaymentStatus.FAILED);
            }

            paymentRepository.save(payment);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error handling payment notification: " + e.getMessage());
        }
    }

    /**
     * Get payment status by order ID
     */
    public Payment getPaymentByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    /**
     * Get payments by reservation ID
     */
    public List<Payment> getPaymentsByReservationId(Long reservationId) {
        return paymentRepository.findByReservationId(reservationId);
    }

    public String getClientKey() {
        return clientKey;
    }

    public Payment getLatestPaymentForReservation(Long reservationId) {
        return paymentRepository.findTop1ByReservationIdOrderByCreatedAtDesc(reservationId)
            .orElse(null);
    }
}
