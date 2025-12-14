package com.sinio.demo.repository;

import com.sinio.demo.dto.RoomSummaryView;
import com.sinio.demo.model.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByNumberIgnoreCase(String number);
    boolean existsByNumberIgnoreCaseAndIdNot(String number, Long id);

    @Query("select new com.sinio.demo.dto.RoomSummaryView(r.id, r.number, r.type, r.status, r.rate) from Room r")
    Page<RoomSummaryView> findSummaries(Pageable pageable);

    default List<RoomSummaryView> findAllSummaries() {
        return findSummaries(Pageable.unpaged()).getContent();
    }
}
