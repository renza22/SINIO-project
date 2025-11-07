package com.sinio.demo.repository;

import com.sinio.demo.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByUser_IdOrderByCreatedAtDesc(Long userId);
    java.util.Optional<Reservation> findByIdAndUser_Id(Long id, Long userId);
}
