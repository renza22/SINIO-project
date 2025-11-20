package com.sinio.demo.service;

import com.sinio.demo.dto.RoomRequest;
import com.sinio.demo.model.Room;
import com.sinio.demo.model.RoomAmenity;
import com.sinio.demo.model.RoomServiceOption;
import com.sinio.demo.model.RoomStatus;
import com.sinio.demo.model.RoomType;
import com.sinio.demo.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
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

        List<RoomAmenity> amenities = parseAmenities(request.getAmenitiesText(), room);
        if (amenities.isEmpty() && room.getId() == null) {
            amenities = defaultAmenityEntities(room, request.getType());
        }
        room.setAmenities(amenities);

        List<RoomServiceOption> options = parseServiceOptions(request.getServicesText(), room);
        if (options.isEmpty() && room.getId() == null) {
            options = defaultServiceOptions(room);
        }
        room.setServiceOptions(options);
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
        request.setAmenitiesText(
            Optional.ofNullable(room.getAmenities()).orElse(List.of()).stream()
                .map(RoomAmenity::getName)
                .collect(Collectors.joining("\n"))
        );
        request.setServicesText(
            Optional.ofNullable(room.getServiceOptions()).orElse(List.of()).stream()
                .sorted(Comparator.comparing(RoomServiceOption::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(RoomServiceOption::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::formatServiceForForm)
                .collect(Collectors.joining("\n"))
        );
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

    @Transactional
    public List<String> resolveAmenities(Room room) {
        List<RoomAmenity> entities = Optional.ofNullable(room.getAmenities()).orElse(List.of());
        List<String> stored = entities.stream()
            .map(RoomAmenity::getName)
            .filter(name -> name != null && !name.isBlank())
            .collect(Collectors.toList());
        if (!stored.isEmpty()) {
            return stored;
        }

        List<String> defaults = getDefaultAmenities(room.getType());
        if (room.getId() == null) {
            return defaults;
        }
        List<RoomAmenity> newEntities = defaultAmenityEntities(room, room.getType());
        room.setAmenities(newEntities);
        Room saved = roomRepository.save(room);
        return Optional.ofNullable(saved.getAmenities()).orElse(newEntities).stream()
            .map(RoomAmenity::getName)
            .collect(Collectors.toList());
    }

    @Transactional
    public List<RoomServiceOption> resolveServiceOptions(Room room) {
        List<RoomServiceOption> stored = Optional.ofNullable(room.getServiceOptions()).orElse(List.of());
        if (!stored.isEmpty()) {
            return sortServices(stored);
        }
        List<RoomServiceOption> defaults = defaultServiceOptions(room);
        if (room.getId() == null) {
            return sortServices(defaults);
        }
        room.setServiceOptions(defaults);
        Room saved = roomRepository.save(room);
        return sortServices(Optional.ofNullable(saved.getServiceOptions()).orElse(defaults));
    }

    private List<RoomServiceOption> sortServices(List<RoomServiceOption> options) {
        return options.stream()
            .sorted(Comparator.comparing(RoomServiceOption::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(RoomServiceOption::getName, String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toList());
    }

    private List<RoomAmenity> parseAmenities(String raw, Room room) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        List<RoomAmenity> amenities = new ArrayList<>();
        String[] lines = raw.split("\\r?\\n");
        int order = 0;
        for (String line : lines) {
            String value = line != null ? line.trim() : "";
            if (value.isEmpty()) {
                continue;
            }
            RoomAmenity amenity = new RoomAmenity();
            amenity.setName(value);
            amenity.setSortOrder(order++);
            amenity.setRoom(room);
            amenities.add(amenity);
        }
        return amenities;
    }

    private List<RoomAmenity> defaultAmenityEntities(Room room, RoomType type) {
        List<String> defaults = getDefaultAmenities(type);
        List<RoomAmenity> amenities = new ArrayList<>();
        for (int i = 0; i < defaults.size(); i++) {
            RoomAmenity amenity = new RoomAmenity();
            amenity.setName(defaults.get(i));
            amenity.setSortOrder(i);
            amenity.setRoom(room);
            amenities.add(amenity);
        }
        return amenities;
    }

    private List<RoomServiceOption> parseServiceOptions(String raw, Room room) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        List<RoomServiceOption> options = new ArrayList<>();
        String[] lines = raw.split("\\r?\\n");
        int nextOrder = 0;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split("\\|");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Format layanan tidak valid pada baris " + (i + 1) + ". Gunakan: Nama | Satuan | Harga");
            }
            String name = parts[0].trim();
            String unit = parts[1].trim();
            String pricePart = parts.length > 2 ? parts[2].trim() : "";

            if (!StringUtils.hasText(name)) {
                throw new IllegalArgumentException("Nama layanan kosong pada baris " + (i + 1) + ".");
            }
            if (!StringUtils.hasText(unit)) {
                unit = "unit";
            }

            BigDecimal price = BigDecimal.ZERO;
            if (StringUtils.hasText(pricePart)) {
                try {
                    price = new BigDecimal(pricePart);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Harga layanan tidak valid pada baris " + (i + 1) + ". Gunakan angka tanpa pemisah.");
                }
            }
            if (price.signum() < 0) {
                throw new IllegalArgumentException("Harga layanan harus bernilai positif pada baris " + (i + 1) + ".");
            }

            RoomServiceOption option = new RoomServiceOption();
            option.setName(name);
            option.setUnit(unit);
            option.setPrice(price);
            option.setSortOrder(nextOrder++);
            option.setRoom(room);
            options.add(option);
        }
        return options;
    }

    private List<RoomServiceOption> defaultServiceOptions(Room room) {
        List<RoomServiceOption> defaults = new ArrayList<>();
        defaults.add(option(room, "Laundry", "kg", 25000, 0));
        defaults.add(option(room, "Room dining", "porsi", 60000, 1));
        defaults.add(option(room, "Spa (60 menit)", "sesi", 180000, 2));
        defaults.add(option(room, "Pembersihan ekstra", "kali", 40000, 3));
        return defaults;
    }

    private RoomServiceOption option(Room room, String name, String unit, int price, int sortOrder) {
        RoomServiceOption option = new RoomServiceOption();
        option.setRoom(room);
        option.setName(name);
        option.setUnit(unit);
        option.setPrice(BigDecimal.valueOf(price));
        option.setSortOrder(sortOrder);
        return option;
    }

    private String formatServiceForForm(RoomServiceOption option) {
        if (option == null) {
            return "";
        }
        String name = option.getName() != null ? option.getName() : "";
        String unit = option.getUnit() != null ? option.getUnit() : "unit";
        BigDecimal price = option.getPrice() != null ? option.getPrice() : BigDecimal.ZERO;
        return name + " | " + unit + " | " + price.stripTrailingZeros().toPlainString();
    }

    private static <T> List<T> concat(List<T> a, List<T> b) {
        return new java.util.ArrayList<>() {{
            addAll(a);
            addAll(b);
        }};
    }
}
