package com.sinio.demo.repository;

import com.sinio.demo.model.Payment;
import com.sinio.demo.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(String orderId);

    List<Payment> findByReservationId(Long reservationId);

    List<Payment> findByStatus(PaymentStatus status);

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findTop1ByReservationIdOrderByCreatedAtDesc(Long reservationId);
}
