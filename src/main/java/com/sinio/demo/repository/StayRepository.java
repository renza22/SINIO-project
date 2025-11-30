package com.sinio.demo.repository;

import com.sinio.demo.model.Stay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StayRepository extends JpaRepository<Stay, Long> {
    Optional<Stay> findByReservation_IdAndCheckoutAtIsNull(Long reservationId);
    List<Stay> findByCheckoutAtIsNull();

    boolean existsByRoom_Id(Long roomId);

    List<Stay> findByRoom_Id(Long roomId);

    List<Stay> findByReservation_Id(Long reservationId);
}
