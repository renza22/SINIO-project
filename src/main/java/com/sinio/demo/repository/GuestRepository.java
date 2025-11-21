package com.sinio.demo.repository;

import com.sinio.demo.model.Guest;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestRepository extends JpaRepository<Guest, Long> {
    Optional<Guest> findByUser_Id(Long userId);
}
