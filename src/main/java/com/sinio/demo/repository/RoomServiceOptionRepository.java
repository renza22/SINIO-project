package com.sinio.demo.repository;

import com.sinio.demo.model.RoomServiceOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomServiceOptionRepository extends JpaRepository<RoomServiceOption, Long> {
    List<RoomServiceOption> findByRoom_IdOrderBySortOrderAscIdAsc(Long roomId);
}
