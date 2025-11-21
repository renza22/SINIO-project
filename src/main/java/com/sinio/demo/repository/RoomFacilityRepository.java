package com.sinio.demo.repository;

import com.sinio.demo.model.RoomFacility;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomFacilityRepository extends JpaRepository<RoomFacility, Long> {
    List<RoomFacility> findByRoom_Id(Long roomId);
}
