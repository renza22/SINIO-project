package com.sinio.demo.repository;

import com.sinio.demo.model.RoomTypeEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomTypeEntityRepository extends JpaRepository<RoomTypeEntity, Long> {
    Optional<RoomTypeEntity> findByCode(String code);
}
