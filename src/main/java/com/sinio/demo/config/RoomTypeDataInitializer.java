package com.sinio.demo.config;

import com.sinio.demo.model.Room;
import com.sinio.demo.model.RoomType;
import com.sinio.demo.model.RoomTypeEntity;
import com.sinio.demo.repository.RoomRepository;
import com.sinio.demo.repository.RoomTypeEntityRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Backfills and keeps master data for tipe_kamar in sync with enum values and existing rooms.
 */
@Component
public class RoomTypeDataInitializer implements CommandLineRunner {

    private final RoomTypeEntityRepository roomTypeEntityRepository;
    private final RoomRepository roomRepository;
    private final JdbcTemplate jdbcTemplate;

    public RoomTypeDataInitializer(
        RoomTypeEntityRepository roomTypeEntityRepository,
        RoomRepository roomRepository,
        JdbcTemplate jdbcTemplate
    ) {
        this.roomTypeEntityRepository = roomTypeEntityRepository;
        this.roomRepository = roomRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        seedMasterTypes();
        backfillRoomTypeLinks();
    }

    private void seedMasterTypes() {
        for (RoomType type : RoomType.values()) {
            roomTypeEntityRepository
                .findByCode(type.name())
                .orElseGet(() -> {
                    RoomTypeEntity entity = new RoomTypeEntity();
                    entity.setCode(type.name());
                    entity.setName(type.getDisplayName());
                    return roomTypeEntityRepository.save(entity);
                });
        }
    }

    private void backfillRoomTypeLinks() {
        List<Room> rooms = roomRepository.findAll();
        for (Room room : rooms) {
            if (room.getId() == null || room.getType() == null) {
                continue;
            }

            RoomTypeEntity master = roomTypeEntityRepository.findByCode(room.getType().name()).orElse(null);
            if (master == null) {
                continue; // should not happen, but keep safe
            }

            try {
                int updated = jdbcTemplate.update(
                    "UPDATE kamar_tipe SET tipe_kamar_id = ? WHERE room_id = ?",
                    master.getId(),
                    room.getId()
                );
                if (updated == 0) {
                    jdbcTemplate.update(
                        "INSERT INTO kamar_tipe (room_id, tipe_kamar_id) VALUES (?, ?)",
                        room.getId(),
                        master.getId()
                    );
                }
            } catch (DataAccessException ignored) {
                // Ignore if legacy schema does not have kamar_tipe; avoids blocking app startup.
            }
        }
    }
}
