package com.sinio.demo.repository;

import com.sinio.demo.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByUser_IdOrderByCreatedAtDesc(Long userId);
    java.util.Optional<Reservation> findByIdAndUser_Id(Long id, Long userId);

    // For occupancy calculation: reservations currently checked-in today
    java.util.List<com.sinio.demo.model.Reservation> findByStatusAndCheckInLessThanEqualAndCheckOutGreaterThan(
        com.sinio.demo.model.ReservationStatus status,
        java.time.LocalDate checkInLte,
        java.time.LocalDate checkOutGt
    );

    java.util.List<com.sinio.demo.model.Reservation> findTop10ByOrderByCreatedAtDesc();

    java.util.List<com.sinio.demo.model.Reservation> findByCheckInEquals(java.time.LocalDate date);
    java.util.List<com.sinio.demo.model.Reservation> findByCheckOutEquals(java.time.LocalDate date);
    java.util.List<com.sinio.demo.model.Reservation> findByCheckInBetween(java.time.LocalDate start, java.time.LocalDate end);
    java.util.List<com.sinio.demo.model.Reservation> findByCheckOutBetween(java.time.LocalDate start, java.time.LocalDate end);
    java.util.List<com.sinio.demo.model.Reservation> findTop10ByStatusInAndCheckInGreaterThanEqualOrderByCheckInAsc(
        java.util.List<com.sinio.demo.model.ReservationStatus> statuses,
        java.time.LocalDate start
    );
    java.util.List<com.sinio.demo.model.Reservation> findTop10ByCheckOutGreaterThanEqualOrderByCheckOutAsc(java.time.LocalDate start);
}
