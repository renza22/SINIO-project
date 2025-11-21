package com.sinio.demo.repository;

import com.sinio.demo.model.ReservationRoom;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRoomRepository extends JpaRepository<ReservationRoom, Long> {
    @EntityGraph(attributePaths = {"room", "reservation"})
    List<ReservationRoom> findByReservation_Id(Long reservationId);
}
