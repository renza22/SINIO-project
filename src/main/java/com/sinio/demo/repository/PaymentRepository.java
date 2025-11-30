package com.sinio.demo.repository;

import com.sinio.demo.model.Payment;
import com.sinio.demo.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    Optional<Payment> findTop1ByReservation_User_IdOrderByCreatedAtDesc(Long userId);

    List<Payment> findTop10ByOrderByCreatedAtDesc();

    List<Payment> findTop10ByStatusInOrderByCreatedAtDesc(List<PaymentStatus> statuses);

    @Query("select coalesce(sum(p.amount),0) from Payment p where p.status = com.sinio.demo.model.PaymentStatus.SUCCESS and p.transactionTime >= :start and p.transactionTime < :end")
    java.math.BigDecimal sumSuccessBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

    @Query("select count(p) from Payment p where p.status = com.sinio.demo.model.PaymentStatus.PENDING")
    long countPendingPayments();
}
