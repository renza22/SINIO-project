package com.sinio.demo.service;

import com.midtrans.Config;
import com.midtrans.ConfigFactory;
import com.midtrans.httpclient.error.MidtransError;
import com.midtrans.service.MidtransSnapApi;
import com.sinio.demo.model.Payment;
import com.sinio.demo.model.PaymentStatus;
import com.sinio.demo.model.Reservation;
import com.sinio.demo.model.ReservationStatus;
import com.sinio.demo.model.Room;
import com.sinio.demo.repository.PaymentRepository;
import com.sinio.demo.repository.ReservationRepository;
import com.sinio.demo.service.ReservationService;
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

    @Autowired
    private ReservationService reservationService;

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
        Payment payment = null;
        for (Payment p : existingPayments) {
            if (p.getStatus() == PaymentStatus.SUCCESS) {
                reservationService.confirmPayment(reservation);
                return p;
            }
            if (p.getStatus() == PaymentStatus.PENDING) {
                // jika minta cash dan sudah ada pending, pakai yang ada
                if ("cash".equals(method)) {
                    return p;
                }
                // jika pending non-Midtrans, kembalikan apa adanya
                if (p.getPaymentType() != null && !"MIDTRANS".equalsIgnoreCase(p.getPaymentType())) {
                    return p;
                }
                // pending midtrans: reuse record tapi regen snap token agar pakai konfigurasi terbaru (qris)
                payment = p;
                break;
            }
        }

        // Calculate total amount
        BigDecimal totalAmount = calculateTotalAmount(reservation);

        // Generate unique order ID
        String orderId = generateOrderId(reservation);

        // Create or reuse payment record
        if (payment == null) {
            payment = new Payment();
            payment.setReservation(reservation);
        }
        payment.setOrderId(orderId);
        payment.setAmount(totalAmount);
        payment.setSnapToken(null);

        // Cash: buat catatan menunggu konfirmasi front office
        if ("cash".equals(method)) {
            payment.setStatus(PaymentStatus.PENDING);
            payment.setPaymentType("CASH");
            payment.setTransactionTime(LocalDateTime.now());
            return paymentRepository.save(payment);
        }

        // Online via Midtrans
        payment.setStatus(PaymentStatus.PENDING);

        // Build Midtrans transaction data
        try {
            Map<String, Object> transactionDetails = buildTransactionDetails(orderId, totalAmount);
            Map<String, Object> customerDetails = buildCustomerDetails(reservation);
            List<Map<String, Object>> itemDetails = buildItemDetails(reservation, totalAmount);

            Config midtransConfig = new Config(serverKey, clientKey, isProduction);
            MidtransSnapApi snapApi = new ConfigFactory(midtransConfig).getSnapApi();

            // Coba prioritas QR lebih dulu; jika gagal (channel unavailable) fallback ke GoPay
            String snapToken;
            try {
                snapToken = snapApi.createTransactionToken(buildSnapParams(transactionDetails, customerDetails, itemDetails, true));
            } catch (Exception primaryEx) {
                snapToken = snapApi.createTransactionToken(buildSnapParams(transactionDetails, customerDetails, itemDetails, false));
            }

            payment.setSnapToken(snapToken);
            payment.setPaymentType("MIDTRANS");
            payment.setTransactionTime(LocalDateTime.now());

            return paymentRepository.save(payment);
        } catch (Exception ex) {
            reservationService.deleteAndRelease(reservation);
            throw ex;
        }
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

    private Map<String, Object> buildSnapParams(Map<String, Object> transactionDetails,
                                               Map<String, Object> customerDetails,
                                               List<Map<String, Object>> itemDetails,
                                               boolean qrOnly) {
        Map<String, Object> params = new HashMap<>();
        params.put("transaction_details", transactionDetails);
        params.put("customer_details", customerDetails);
        params.put("item_details", itemDetails);
        if (qrOnly) {
            params.put("enabled_payments", java.util.List.of("qris", "other_qris"));
        } else {
            params.put("enabled_payments", java.util.List.of("qris", "other_qris", "gopay"));
        }
        return params;
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
            syncReservationWithPayment(payment);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error handling payment notification: " + e.getMessage());
        }
    }

    private void syncReservationWithPayment(Payment payment) {
        if (payment == null) {
            return;
        }
        Reservation reservation = payment.getReservation();
        if (reservation == null) {
            return;
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            reservationService.confirmPayment(reservation);
            return;
        }
        if (payment.getStatus() == PaymentStatus.CANCELLED
            || payment.getStatus() == PaymentStatus.EXPIRED
            || payment.getStatus() == PaymentStatus.FAILED) {
            discardReservation(reservation);
        }
    }

    private void discardReservation(Reservation reservation) {
        if (reservation == null) {
            return;
        }
        reservationService.deleteAndRelease(reservation);
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

    /**
     * Tagihan yang masih harus dibayar oleh tamu (menggabungkan reservasi yang belum lunas).
     */
    public List<Map<String, Object>> getPendingBillsForUser(Long userId) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
        List<Reservation> reservations = reservationRepository.findByUser_IdOrderByCreatedAtDesc(userId);
        List<Map<String, Object>> bills = new ArrayList<>();
        for (Reservation r : reservations) {
            if (r.getStatus() == ReservationStatus.CANCELED || r.getStatus() == ReservationStatus.CHECKED_OUT) {
                continue; // abaikan yang sudah selesai/dibatalkan
            }
            if (r.getRoom() == null) {
                continue; // data tidak lengkap, jangan tampilkan
            }
            Payment latest = getLatestPaymentForReservation(r.getId());
            if (latest != null && latest.getStatus() == PaymentStatus.SUCCESS) {
                continue; // sudah lunas
            }
            BigDecimal amount = calculateTotalAmount(r);
            Map<String, Object> m = new HashMap<>();
            m.put("reservationId", r.getId());
            m.put("kode", r.getCode());
            m.put("kamar", r.getRoom() != null ? r.getRoom().getNumber() : "-");
            m.put("tipe", r.getRoom() != null && r.getRoom().getType() != null ? r.getRoom().getType().getDisplayName() : "-");
            m.put("periode", fmt.format(r.getCheckIn()) + " - " + fmt.format(r.getCheckOut()));
            m.put("status", r.getStatus().name());
            m.put("paymentStatus", latest != null ? latest.getStatus().name() : PaymentStatus.PENDING.name());
            m.put("paymentType", latest != null ? latest.getPaymentType() : null);
            m.put("orderId", latest != null ? latest.getOrderId() : null);
            m.put("amount", amount);
            bills.add(m);
        }
        return bills;
    }

    public Payment staffConfirmPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("Payment not found"));
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            payment.setStatus(PaymentStatus.SUCCESS);
            if (payment.getTransactionTime() == null) {
                payment.setTransactionTime(LocalDateTime.now());
            }
            if (payment.getPaymentType() == null) {
                payment.setPaymentType("CASH");
            }
            paymentRepository.save(payment);
            syncReservationWithPayment(payment);
        }
        return payment;
    }

    public List<Map<String, Object>> getPendingPaymentViewsForStaff(int limit) {
        List<Payment> payments = paymentRepository.findTop10ByStatusInOrderByCreatedAtDesc(
            List.of(PaymentStatus.PENDING)
        );
        if (limit > 0 && payments.size() > limit) {
            payments = payments.subList(0, limit);
        }
        return payments.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("paymentId", p.getId());
            m.put("orderId", p.getOrderId());
            m.put("metode", p.getPaymentType() != null ? p.getPaymentType() : "UNKNOWN");
            m.put("jumlah", p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO);
            m.put("dibuat", p.getCreatedAt());
            Reservation res = p.getReservation();
            if (res != null) {
                m.put("reservasiId", res.getId());
                m.put("kode", res.getCode());
                m.put("tamu", res.getUser().getFullName());
                m.put("kamar", res.getRoom().getNumber());
                m.put("periode", res.getCheckIn() + " - " + res.getCheckOut());
            }
            return m;
        }).toList();
    }

    public List<Map<String, Object>> getRecentPaymentViews(int limit) {
        List<Payment> payments = paymentRepository.findTop10ByOrderByCreatedAtDesc();
        if (limit > 0 && payments.size() > limit) {
            payments = payments.subList(0, limit);
        }
        return payments.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("kodeInvoice", p.getOrderId() != null ? p.getOrderId() : ("PAY-" + p.getId()));
            m.put("metodeNama", p.getPaymentType() != null ? p.getPaymentType() : "UNKNOWN");
            m.put("tanggal", p.getTransactionTime() != null ? p.getTransactionTime() : p.getCreatedAt());
            m.put("jumlah", p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO);
            String status = switch (p.getStatus()) {
                case SUCCESS -> "BERHASIL";
                case PENDING -> "PENDING";
                case CANCELLED -> "DIBATALKAN";
                case EXPIRED -> "KADALUARSA";
                case FAILED -> "GAGAL";
            };
            m.put("status", status);
            return m;
        }).toList();
    }

    public BigDecimal getTodayRevenue() {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDateTime start = today.atStartOfDay();
        java.time.LocalDateTime end = start.plusDays(1);
        BigDecimal sum = paymentRepository.sumSuccessBetween(start, end);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    public long getPendingPaymentCount() {
        return paymentRepository.countPendingPayments();
    }

    /**
     * Ambil pembayaran terbaru milik tamu (untuk "Tagihan Terbaru" di dashboard tamu).
     */
    public Map<String, Object> getLatestPaymentViewForUser(Long userId) {
        // Hanya tampilkan tagihan pending untuk pembayaran non-cash (online)
        return paymentRepository.findTop10ByStatusInOrderByCreatedAtDesc(List.of(PaymentStatus.PENDING))
            .stream()
            .filter(p -> p.getReservation() != null && p.getReservation().getUser() != null && p.getReservation().getUser().getId().equals(userId))
            .filter(p -> p.getPaymentType() != null && !"CASH".equalsIgnoreCase(p.getPaymentType()))
            .findFirst()
            .map(p -> {
                Map<String, Object> m = new HashMap<>();
                m.put("invoiceId", p.getId());
                m.put("kodeInvoice", p.getOrderId() != null ? p.getOrderId() : ("INV-" + p.getId()));
                m.put("tanggal", p.getTransactionTime() != null ? p.getTransactionTime() : p.getCreatedAt());
                m.put("total", p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO);
                m.put("status", "MENUNGGU");
                return m;
            })
            .orElse(null);
    }

    /**
     * Tandai pembayaran Midtrans berhasil berdasarkan orderId (fallback jika webhook tidak dipicu).
     */
    @Transactional
    public Payment markSuccessFromClient(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID tidak boleh kosong.");
        }
        Payment payment = paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new RuntimeException("Payment not found"));
        // Untuk pembayaran cash, konfirmasi hanya boleh dari karyawan/front office
        if (payment.getPaymentType() != null && "CASH".equalsIgnoreCase(payment.getPaymentType())) {
            return payment;
        }
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            payment.setStatus(PaymentStatus.SUCCESS);
            if (payment.getTransactionTime() == null) {
                payment.setTransactionTime(LocalDateTime.now());
            }
            paymentRepository.save(payment);
            syncReservationWithPayment(payment);
        }
        return payment;
    }
}
