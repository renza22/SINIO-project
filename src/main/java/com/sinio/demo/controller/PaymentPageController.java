package com.sinio.demo.controller;

import com.sinio.demo.model.Payment;
import com.sinio.demo.model.Reservation;
import com.sinio.demo.service.PaymentService;
import com.sinio.demo.service.ReservationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Controller
public class PaymentPageController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ReservationService reservationService;

    /**
     * Halaman payment untuk reservasi
     */
    @GetMapping("/guest/payment/{reservationId}")
    public String paymentPage(@PathVariable Long reservationId, HttpSession session,
            RedirectAttributes redirectAttributes, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            redirectAttributes.addFlashAttribute("loginError", "Silakan login terlebih dahulu.");
            return "redirect:/login";
        }

        // Verify reservation belongs to user
        return reservationService.findByUser(userId)
                .stream()
                .filter(r -> r.getId().equals(reservationId))
                .findFirst()
                .map(reservation -> {
                    model.addAttribute("userName", session.getAttribute("userName"));
                    model.addAttribute("userEmail", session.getAttribute("userEmail"));
                    model.addAttribute("reservation", reservation);
                    model.addAttribute("totalAmount", paymentService.calculateTotalAmount(reservation));
                    model.addAttribute("clientKey", paymentService.getClientKey());

                    // Check existing payment
                    List<Payment> payments = paymentService.getPaymentsByReservationId(reservationId);
                    if (!payments.isEmpty()) {
                        model.addAttribute("payment", payments.get(0));
                    }

                    return "guest_payment";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("guestError", "Reservasi tidak ditemukan.");
                    return "redirect:/guest/reservasi";
                });
    }

    /**
     * Success page
     */
    @GetMapping("/payment/success")
    public String paymentSuccess(@RequestParam(required = false) String order_id, HttpSession session, Model model) {
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("userEmail", session.getAttribute("userEmail"));
        model.addAttribute("orderId", order_id);
        try {
            Payment payment = paymentService.markSuccessFromClient(order_id);
            boolean isCash = payment != null && "CASH".equalsIgnoreCase(payment.getPaymentType());
            model.addAttribute("isCashPayment", isCash);
            model.addAttribute("paymentType", payment != null ? payment.getPaymentType() : null);
            model.addAttribute("payment", payment);
            if (payment != null && payment.getReservation() != null) {
                Reservation resv = payment.getReservation();
                model.addAttribute("reservation", resv);
                model.addAttribute("receiptLink", "/payment/receipt?order_id=" + payment.getOrderId());
            }
        } catch (Exception ignored) {
            model.addAttribute("isCashPayment", false);
        }
        return "payment_success";
    }

    /**
     * Pending page
     */
    @GetMapping("/payment/pending")
    public String paymentPend(@RequestParam(required = false) String order_id, HttpSession session, Model model) {
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("userEmail", session.getAttribute("userEmail"));
        model.addAttribute("orderId", order_id);
        return "payment_pending";
    }

    /**
     * Error page
     */
    @GetMapping("/payment/error")
    public String paymentError(@RequestParam(required = false) String order_id, HttpSession session, Model model) {
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("userEmail", session.getAttribute("userEmail"));
        model.addAttribute("orderId", order_id);
        return "payment_error";
    }

    /**
     * Printable receipt
     */
    @GetMapping("/payment/receipt")
    public String paymentReceipt(@RequestParam("order_id") String orderId, Model model) {
        Payment payment = paymentService.getPaymentByOrderId(orderId);
        Reservation reservation = payment.getReservation();
        long nights = ChronoUnit.DAYS.between(reservation.getCheckIn(), reservation.getCheckOut());
        if (nights <= 0) nights = 1;
        var roomTotal = reservation.getRoom().getRate().multiply(java.math.BigDecimal.valueOf(nights));
        var serviceTotal = reservation.getRequestedServicesTotal();
        var amount = payment.getAmount() != null ? payment.getAmount() : roomTotal.add(serviceTotal);

        model.addAttribute("payment", payment);
        model.addAttribute("reservation", reservation);
        model.addAttribute("nights", nights);
        model.addAttribute("roomTotal", roomTotal);
        model.addAttribute("serviceTotal", serviceTotal);
        model.addAttribute("paymentAmount", amount);
        return "payment_receipt";
    }
}
