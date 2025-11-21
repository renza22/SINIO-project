package com.sinio.demo.service;

import com.sinio.demo.dto.RoomRequest;
import com.sinio.demo.model.Room;
import com.sinio.demo.model.RoomAmenity;
import com.sinio.demo.model.RoomServiceOption;
import com.sinio.demo.model.RoomStatus;
import com.sinio.demo.model.RoomType;
import com.sinio.demo.repository.FacilityRepository;
import com.sinio.demo.repository.RoomFacilityRepository;
import com.sinio.demo.repository.RoomRepository;
import com.sinio.demo.repository.RoomTypeEntityRepository;
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
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private static final DateTimeFormatter ACTIVITY_TIME_FORMATTER =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(new Locale.Builder().setLanguage("id").setRegion("ID").build());

    private final RoomRepository roomRepository;
    private final FacilityRepository facilityRepository;
    private final RoomFacilityRepository roomFacilityRepository;
    private final RoomTypeEntityRepository roomTypeEntityRepository;

    public RoomService(
        RoomRepository roomRepository,
        FacilityRepository facilityRepository,
        RoomFacilityRepository roomFacilityRepository,
        RoomTypeEntityRepository roomTypeEntityRepository
    ) {
        this.roomRepository = roomRepository;
        this.facilityRepository = facilityRepository;
        this.roomFacilityRepository = roomFacilityRepository;
        this.roomTypeEntityRepository = roomTypeEntityRepository;
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
            statEntry("Total Kamar", total == 0 ? "-" : String.valueOf(total), "Suite, Deluxe, Superior"),
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
        return "Kamar " + room.getNumber() + " - " + room.getType().getDisplayName();
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

        List<String> facilityNames = parseFacilityNames(request.getAmenitiesText());
        Room room = new Room();
        applyRequest(room, request, facilityNames);
        room.setNumber(normalizedNumber);
        Room saved = roomRepository.save(room);
        syncRoomFacilities(saved, facilityNames);
        return saved;
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

        List<String> facilityNames = parseFacilityNames(request.getAmenitiesText());
        applyRequest(room, request, facilityNames);
        room.setNumber(normalizedNumber);
        Room saved = roomRepository.save(room);
        syncRoomFacilities(saved, facilityNames);
        return saved;
    }

    @Transactional
    public void deleteRoom(Long id) {
        Room room = roomRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Kamar tidak ditemukan."));
        roomRepository.delete(room);
    }

    private void applyRequest(Room room, RoomRequest request, List<String> facilityNames) {
        room.setType(request.getType());
        room.setRate(request.getRate());
        room.setStatus(request.getStatus());
        room.setNote(request.getNote());
        room.setLastCleanedAt(request.getLastCleanedAt());

        List<String> amenitiesNames = facilityNames;
        if (amenitiesNames == null || amenitiesNames.isEmpty()) {
            amenitiesNames = getDefaultAmenities(request.getType());
        }
        room.setAmenities(toAmenities(amenitiesNames, room));

        List<RoomServiceOption> options = parseServiceOptions(request.getServicesText(), room);
        if (options.isEmpty() && room.getId() == null) {
            options = defaultServiceOptions(room);
        }
        room.setServiceOptions(options);
    }

    public List<RoomType> getRoomTypes() {
        List<RoomType> enums = RoomType.defaultOrder();
        var masters = roomTypeEntityRepository.findAll();
        if (masters.isEmpty()) {
            return enums;
        }
        // map by code; fall back to enums for unknown codes
        List<RoomType> ordered = new ArrayList<>();
        masters.forEach(m -> {
            try {
                ordered.add(RoomType.valueOf(m.getCode()));
            } catch (IllegalArgumentException ignored) {
                // ignore unknown codes to avoid breaking forms
            }
        });
        if (ordered.isEmpty()) {
            return enums;
        }
        return ordered;
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
        request.setAmenitiesText(String.join("\n", resolveFacilityNames(room)));
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

    public List<String> getFacilityOptions() {
        List<String> options = facilityRepository.findAll().stream()
            .map(com.sinio.demo.model.Facility::getName)
            .filter(StringUtils::hasText)
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
        if (!options.isEmpty()) {
            return options;
        }
        // fallback to defaults so UI is never empty
        return getDefaultAmenities(RoomType.STANDARD);
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
        List<String> facilities = resolveFacilityNames(room);
        if (!facilities.isEmpty()) {
            return facilities;
        }
        List<String> defaults = getDefaultAmenities(room.getType());
        if (room.getId() == null) {
            return defaults;
        }
        syncRoomFacilities(room, defaults);
        room.setAmenities(toAmenities(defaults, room));
        Room saved = roomRepository.save(room);
        return resolveFacilityNames(saved);
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

    private List<String> parseFacilityNames(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        String[] lines = raw.split("\\r?\\n");
        List<String> names = new ArrayList<>();
        for (String line : lines) {
            String value = line != null ? line.trim() : "";
            if (!value.isEmpty()) {
                names.add(value);
            }
        }
        return names;
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

    private List<RoomAmenity> toAmenities(List<String> names, Room room) {
        List<RoomAmenity> amenities = new ArrayList<>();
        int order = 0;
        for (String name : names) {
            RoomAmenity amenity = new RoomAmenity();
            amenity.setName(name);
            amenity.setSortOrder(order++);
            amenity.setRoom(room);
            amenities.add(amenity);
        }
        return amenities;
    }

    private List<String> resolveFacilityNames(Room room) {
        if (room.getId() != null) {
            List<String> fromJoins = roomFacilityRepository.findByRoom_Id(room.getId()).stream()
                .map(rf -> rf.getFacility().getName())
                .filter(StringUtils::hasText)
                .toList();
            if (!fromJoins.isEmpty()) {
                return fromJoins;
            }
        }
        return Optional.ofNullable(room.getAmenities()).orElse(List.of()).stream()
            .map(RoomAmenity::getName)
            .filter(name -> name != null && !name.isBlank())
            .toList();
    }

    @Transactional
    void syncRoomFacilities(Room room, List<String> names) {
        if (room == null || room.getId() == null) {
            return;
        }
        List<String> distinct = Optional.ofNullable(names).orElse(List.of())
            .stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .distinct()
            .toList();

        // Ensure facilities exist
        List<com.sinio.demo.model.Facility> facilities = new ArrayList<>();
        for (String name : distinct) {
            com.sinio.demo.model.Facility facility = facilityRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    com.sinio.demo.model.Facility f = new com.sinio.demo.model.Facility();
                    f.setName(name);
                    return facilityRepository.save(f);
                });
            facilities.add(facility);
        }

        // Current joins
        List<com.sinio.demo.model.RoomFacility> current = roomFacilityRepository.findByRoom_Id(room.getId());
        Map<Long, com.sinio.demo.model.RoomFacility> byFacilityId = current.stream()
            .collect(Collectors.toMap(rf -> rf.getFacility().getId(), rf -> rf));

        // Add missing joins
        for (com.sinio.demo.model.Facility facility : facilities) {
            if (!byFacilityId.containsKey(facility.getId())) {
                com.sinio.demo.model.RoomFacility rf = new com.sinio.demo.model.RoomFacility();
                rf.setRoom(room);
                rf.setFacility(facility);
                roomFacilityRepository.save(rf);
            }
        }

        // Remove joins not in desired set
        Set<Long> desiredIds = facilities.stream().map(com.sinio.demo.model.Facility::getId).collect(Collectors.toSet());
        current.stream()
            .filter(rf -> !desiredIds.contains(rf.getFacility().getId()))
            .forEach(roomFacilityRepository::delete);
    }

    private static <T> List<T> concat(List<T> a, List<T> b) {
        return new java.util.ArrayList<>() {{
            addAll(a);
            addAll(b);
        }};
    }
}

