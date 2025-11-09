package com.sinio.demo.service;

import com.sinio.demo.dto.RoomRequest;
import com.sinio.demo.model.Room;
import com.sinio.demo.model.RoomStatus;
import com.sinio.demo.model.RoomType;
import com.sinio.demo.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private static final DateTimeFormatter ACTIVITY_TIME_FORMATTER =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(new Locale.Builder().setLanguage("id").setRegion("ID").build());

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public List<Room> findAllSorted() {
        return roomRepository.findAll()
            .stream()
            .sorted(Comparator.comparing(Room::getNumber, String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toList());
    }

    public List<Map<String, Object>> buildStats(List<Room> rooms) {
        long total = rooms.size();
        long available = rooms.stream().filter(room -> room.getStatus() == RoomStatus.AVAILABLE).count();
        long booked = rooms.stream().filter(room -> room.getStatus() == RoomStatus.BOOKED).count();
        long maintenance = rooms.stream().filter(room -> room.getStatus() == RoomStatus.MAINTENANCE).count();

        return List.of(
            statEntry("Total Kamar", total == 0 ? "—" : String.valueOf(total), "Suite, Deluxe, Superior"),
            statEntry("Tersedia", String.valueOf(available), "Siap menerima tamu hari ini"),
            statEntry("Terbooking", String.valueOf(booked), "Check-in terjadwal pekan ini"),
            statEntry("Perawatan", String.valueOf(maintenance), "Sedang dijadwalkan housekeeping")
        );
    }

    private Map<String, Object> statEntry(String label, String value, String description) {
        Map<String, Object> map = new HashMap<>();
        map.put("label", label);
        map.put("value", value);
        map.put("description", description);
        return map;
    }

    public List<Map<String, String>> buildActivities(List<Room> rooms) {
        return rooms.stream()
            .sorted(Comparator.comparing(Room::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
            .limit(5)
            .map(room -> {
                Map<String, String> activity = new HashMap<>();
                activity.put("title", buildActivityTitle(room));
                activity.put("description", buildActivityDescription(room));
                activity.put("time", room.getUpdatedAt() != null ? ACTIVITY_TIME_FORMATTER.format(room.getUpdatedAt()) : "Waktu belum tersedia");
                return activity;
            })
            .collect(Collectors.toList());
    }

    private String buildActivityTitle(Room room) {
        return "Kamar " + room.getNumber() + " — " + room.getType().getDisplayName();
    }

    private String buildActivityDescription(Room room) {
        StringBuilder builder = new StringBuilder(room.getStatus().getDisplayName());
        if (room.getLastCleanedAt() != null) {
            builder.append(" • Dibersihkan ").append(ACTIVITY_TIME_FORMATTER.format(room.getLastCleanedAt()));
        }
        if (room.getNote() != null && !room.getNote().isBlank()) {
            builder.append(" • ").append(room.getNote());
        }
        return builder.toString();
    }

    @Transactional
    public Room createRoom(RoomRequest request) {
        String normalizedNumber = request.getNumber().trim();
        roomRepository.findByNumberIgnoreCase(normalizedNumber).ifPresent(room -> {
            throw new IllegalArgumentException("Nomor kamar sudah digunakan.");
        });

        Room room = new Room();
        applyRequest(room, request);
        room.setNumber(normalizedNumber);
        return roomRepository.save(room);
    }

    @Transactional
    public Room updateRoom(Long id, RoomRequest request) {
        Room room = roomRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Kamar tidak ditemukan."));

        String normalizedNumber = request.getNumber().trim();
        Optional<Room> existing = roomRepository.findByNumberIgnoreCase(normalizedNumber);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new IllegalArgumentException("Nomor kamar sudah digunakan.");
        }

        applyRequest(room, request);
        room.setNumber(normalizedNumber);
        return roomRepository.save(room);
    }

    @Transactional
    public void deleteRoom(Long id) {
        Room room = roomRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Kamar tidak ditemukan."));
        roomRepository.delete(room);
    }

    private void applyRequest(Room room, RoomRequest request) {
        room.setType(request.getType());
        room.setRate(request.getRate());
        room.setStatus(request.getStatus());
        room.setNote(request.getNote());
        room.setLastCleanedAt(request.getLastCleanedAt());
    }

    public List<RoomType> getRoomTypes() {
        return RoomType.defaultOrder();
    }

    public RoomRequest toRequest(Room room) {
        RoomRequest request = new RoomRequest();
        request.setId(room.getId());
        request.setNumber(room.getNumber());
        request.setType(room.getType());
        request.setRate(room.getRate());
        request.setStatus(room.getStatus());
        request.setNote(room.getNote());
        request.setLastCleanedAt(room.getLastCleanedAt());
        return request;
    }

    public List<RoomStatus> getRoomStatuses() {
        return List.of(RoomStatus.values());
    }

    public Optional<Room> findById(Long id) {
        return roomRepository.findById(id);
    }

    // --- Guest utilities (non-persistent helpers) ---
    public List<String> getDefaultAmenities(RoomType type) {
        // Basic amenities for all rooms
        List<String> base = List.of(
            "Wi-Fi berkecepatan tinggi",
            "TV LED kabel",
            "AC",
            "Air minum & teh/kopi",
            "Kamar mandi dalam + shower air panas"
        );

        return switch (type) {
            case DELUXE_KING, DELUXE_TWIN, SUPERIOR_TWIN, STANDARD -> base;
            case STUDIO_LOFT -> concat(base, List.of("Mezzanine/loteng", "Dapur kecil"));
            case EXECUTIVE_SUITE -> concat(base, List.of("Living room terpisah", "Meja kerja", "Bathtub"));
            case FAMILY_ROOM -> concat(base, List.of("Ranjang tambahan", "Area keluarga"));
            case SUITE_PANORAMA -> concat(base, List.of("Pemandangan kota/pantai", "Living room", "Bathtub"));
            case PRESIDENTIAL_SUITE, VILLA -> concat(base, List.of("Ruang tamu luas", "Ruang makan", "Bathtub & shower terpisah"));
        };
    }

    public List<Map<String, Object>> getDefaultRoomServices() {
        return List.of(
            service("Laundry", "kg", 25000),
            service("Room dining", "porsi", 60000),
            service("Spa (60 menit)", "sesi", 180000),
            service("Pembersihan ekstra", "kali", 40000)
        );
    }

    private Map<String, Object> service(String name, String unit, int price) {
        Map<String, Object> m = new HashMap<>();
        m.put("nama", name);
        m.put("satuan", unit);
        m.put("hargaSatuan", price);
        return m;
    }

    private static <T> List<T> concat(List<T> a, List<T> b) {
        return new java.util.ArrayList<>() {{
            addAll(a);
            addAll(b);
        }};
    }
}
