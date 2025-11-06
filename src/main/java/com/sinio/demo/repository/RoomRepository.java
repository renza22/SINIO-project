package com.sinio.demo.repository;

import com.sinio.demo.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByNumberIgnoreCase(String number);
    boolean existsByNumberIgnoreCaseAndIdNot(String number, Long id);
}
