package com.sinio.demo.service;

import com.sinio.demo.dto.RoomRequest;
import com.sinio.demo.dto.RoomSummaryView;
import com.sinio.demo.model.Room;
import com.sinio.demo.model.RoomAmenity;
import com.sinio.demo.model.RoomImage;
import com.sinio.demo.model.RoomServiceOption;
import com.sinio.demo.model.RoomStatus;
import com.sinio.demo.model.RoomType;
import com.sinio.demo.model.RoomTypeEntity;
import com.sinio.demo.repository.FacilityRepository;
import com.sinio.demo.repository.PaymentRepository;
import com.sinio.demo.repository.RoomFacilityRepository;
import com.sinio.demo.repository.RoomRepository;
import com.sinio.demo.repository.RoomTypeEntityRepository;
import com.sinio.demo.repository.ReservationRepository;
import com.sinio.demo.repository.ReservationRoomRepository;
import com.sinio.demo.repository.StayRepository;
import com.sinio.demo.model.Facility;
import com.sinio.demo.repository.RoomServiceOptionRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
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
    private final ReservationRepository reservationRepository;
    private final ReservationRoomRepository reservationRoomRepository;
    private final StayRepository stayRepository;
    private final PaymentRepository paymentRepository;
    private final RoomServiceOptionRepository roomServiceOptionRepository;
    private final JdbcTemplate jdbcTemplate;

    public RoomService(
        RoomRepository roomRepository,
        FacilityRepository facilityRepository,
        RoomFacilityRepository roomFacilityRepository,
        RoomTypeEntityRepository roomTypeEntityRepository,
        ReservationRepository reservationRepository,
        ReservationRoomRepository reservationRoomRepository,
        StayRepository stayRepository,
        PaymentRepository paymentRepository,
        RoomServiceOptionRepository roomServiceOptionRepository,
        JdbcTemplate jdbcTemplate
    ) {
        this.roomRepository = roomRepository;
        this.facilityRepository = facilityRepository;
        this.roomFacilityRepository = roomFacilityRepository;
        this.roomTypeEntityRepository = roomTypeEntityRepository;
        this.reservationRepository = reservationRepository;
        this.reservationRoomRepository = reservationRoomRepository;
        this.stayRepository = stayRepository;
        this.paymentRepository = paymentRepository;
        this.roomServiceOptionRepository = roomServiceOptionRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Room> findAllSorted() {
        return roomRepository.findAll(Sort.by(Sort.Order.by("number").ignoreCase()));
    }

    @Transactional
    public Room markAvailableAfterCleaning(Long roomId) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Kamar tidak ditemukan."));
        if (room.getStatus() != RoomStatus.CLEANING) {
            throw new IllegalStateException("Kamar tidak dalam status pembersihan.");
        }
        room.setStatus(RoomStatus.AVAILABLE);
        room.setLastCleanedAt(LocalDateTime.now());
        return roomRepository.save(room);
    }

    public List<RoomSummaryView> findGuestSummaries() {
        // Default sort by room number for backward compatibility
        return roomRepository.findSummaries(PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Order.by("number").ignoreCase())))
            .getContent();
    }

    public Page<RoomSummaryView> findGuestSummaries(int page, int size) {
        return findGuestSummaries(page, size, "number");
    }

    public Page<RoomSummaryView> findGuestSummaries(int page, int size, String sortOption) {
        int safeSize = Math.max(6, Math.min(size, 30)); // guard extremes to keep responses small
        int safePage = Math.max(page, 0);
        Sort sort = resolveGuestSort(sortOption);
        Pageable pageable = PageRequest.of(safePage, safeSize, sort);
        return roomRepository.findSummaries(pageable);
    }

    private Sort resolveGuestSort(String sortOption) {
        String key = sortOption == null ? "" : sortOption.trim().toLowerCase(Locale.ROOT);
        return switch (key) {
            case "type", "jenis" -> Sort.by(
                Sort.Order.by("type"),
                Sort.Order.by("number").ignoreCase()
            );
            case "price-desc", "harga-desc", "rate-desc" -> Sort.by(
                new Sort.Order(Sort.Direction.DESC, "rate"),
                Sort.Order.by("number").ignoreCase()
            );
            case "price-asc", "harga-asc", "rate-asc" -> Sort.by(
                Sort.Order.by("rate"),
                Sort.Order.by("number").ignoreCase()
            );
            default -> Sort.by(Sort.Order.by("number").ignoreCase());
        };
    }

    public List<RoomServiceOption> findAllServiceOptions() {
        return roomServiceOptionRepository.findAll(Sort.by(Sort.Order.by("name").ignoreCase()));
    }

    public List<RoomServiceOption> findDistinctServiceOptions() {
        Map<String, RoomServiceOption> byKey = new LinkedHashMap<>();
        roomServiceOptionRepository.findAll(Sort.by(Sort.Order.by("name").ignoreCase()))
            .forEach(opt -> {
                String key = (opt.getName() == null ? "" : opt.getName().trim().toLowerCase(Locale.ROOT))
                    + "|" + (opt.getPrice() != null ? opt.getPrice().toPlainString() : "0");
                byKey.putIfAbsent(key, opt);
            });
        return new ArrayList<>(byKey.values());
    }

    public List<RoomSummaryView> findFeaturedSummaries(int limit) {
        if (limit > 0) {
            Pageable page = PageRequest.of(0, limit, Sort.by(Sort.Order.by("number").ignoreCase()));
            return roomRepository.findSummaries(page).getContent();
        }
        return roomRepository.findAllSummaries();
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

    @Transactional
    public void reconcileRoomStatuses() {
        List<Room> rooms = roomRepository.findAll();
        LocalDate today = LocalDate.now();
        for (Room room : rooms) {
            boolean hasActiveStay = stayRepository.findByRoom_Id(room.getId())
                .stream()
                .anyMatch(stay -> stay.getCheckoutAt() == null);

            boolean hasUpcomingReservation = reservationRepository.findByRoom_Id(room.getId())
                .stream()
                .anyMatch(res -> (res.getStatus() == com.sinio.demo.model.ReservationStatus.BOOKED
                    || res.getStatus() == com.sinio.demo.model.ReservationStatus.CONFIRMED)
                    && (res.getCheckOut() == null || !res.getCheckOut().isBefore(today)));

            RoomStatus desiredStatus = room.getStatus();
            if (hasActiveStay) {
                desiredStatus = RoomStatus.OCCUPIED;
            } else if (hasUpcomingReservation) {
                desiredStatus = RoomStatus.BOOKED;
            } else if (room.getStatus() == RoomStatus.OCCUPIED || room.getStatus() == RoomStatus.BOOKED) {
                desiredStatus = RoomStatus.AVAILABLE;
            }

            if (desiredStatus != room.getStatus()) {
                room.setStatus(desiredStatus);
                roomRepository.save(room);
            }
        }
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
        syncRoomType(saved);
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
        syncRoomType(saved);
        return saved;
    }

    @Transactional
    public void deleteRoom(Long id) {
        Room room = roomRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Kamar tidak ditemukan."));

        // Hapus seluruh jejak relasi agar admin bisa menghapus kamar dalam kondisi apapun
        List<com.sinio.demo.model.ReservationRoom> reservationRooms = reservationRoomRepository.findByRoom_Id(id);
        Set<Long> reservationIds = new HashSet<>();
        reservationRepository.findByRoom_Id(id).forEach(r -> reservationIds.add(r.getId()));
        reservationRooms.stream()
            .map(rr -> rr.getReservation() != null ? rr.getReservation().getId() : null)
            .filter(Objects::nonNull)
            .forEach(reservationIds::add);

        // Bersihkan entitas terkait reservasi dulu (stays, payments, reservation_rooms, reservations)
        for (Long resId : reservationIds) {
            stayRepository.deleteAll(stayRepository.findByReservation_Id(resId));
            paymentRepository.deleteAll(paymentRepository.findByReservationId(resId));
        }
        reservationRoomRepository.deleteAll(reservationRooms);
        if (!reservationIds.isEmpty()) {
            reservationRepository.deleteAll(reservationRepository.findAllById(reservationIds));
        }

        // Stays yang langsung refer ke room (jaga-jaga ada sisa)
        stayRepository.deleteAll(stayRepository.findByRoom_Id(id));

        // Join fasilitas
        roomFacilityRepository.deleteAll(roomFacilityRepository.findByRoom_Id(id));

        // Bersihkan tabel legacy yang mungkin ada FK ke rooms, tapi abaikan jika tabel belum ada
        try {
            jdbcTemplate.update("DELETE FROM kamar_tipe WHERE room_id = ?", id);
        } catch (DataAccessException ignored) {
            // abaikan jika tabel belum ada di skema lama
        }

        roomRepository.delete(room);
    }

    private void applyRequest(Room room, RoomRequest request, List<String> facilityNames) {
        room.setType(request.getType());
        room.setRate(request.getRate());
        room.setStatus(request.getStatus());
        room.setMaxOccupancy(request.getMaxOccupancy() != null ? request.getMaxOccupancy() : 2);
        room.setNote(request.getNote());
        room.setLastCleanedAt(request.getLastCleanedAt());

        List<String> amenitiesNames = facilityNames;
        if (amenitiesNames == null || amenitiesNames.isEmpty()) {
            amenitiesNames = getDefaultAmenities(request.getType());
        }
        room.setAmenities(toAmenities(amenitiesNames, room));
        room.setImages(toImages(parseImageEntries(request.getImageUrlsText()), room));
        // Layanan berbayar kini dikelola via halaman khusus (/admin/layanan), sehingga form kamar tidak lagi mengubahnya.
    }

    @Transactional
    public RoomServiceOption addServiceOption(Long roomId, String name, String unit, BigDecimal price) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Kamar tidak ditemukan."));
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Nama layanan wajib diisi.");
        }
        String safeUnit = StringUtils.hasText(unit) ? unit.trim() : "unit";
        BigDecimal safePrice = price != null ? price : BigDecimal.ZERO;
        if (safePrice.signum() < 0) {
            throw new IllegalArgumentException("Harga layanan tidak boleh negatif.");
        }
        Integer nextOrder = roomServiceOptionRepository.findByRoom_IdOrderBySortOrderAscIdAsc(roomId).stream()
            .map(RoomServiceOption::getSortOrder)
            .filter(Objects::nonNull)
            .max(Integer::compareTo)
            .map(v -> v + 1)
            .orElse(0);

        RoomServiceOption option = new RoomServiceOption();
        option.setRoom(room);
        option.setName(name.trim());
        option.setUnit(safeUnit);
        option.setPrice(safePrice);
        option.setSortOrder(nextOrder);
        return roomServiceOptionRepository.save(option);
    }

    @Transactional
    public RoomServiceOption updateServiceOption(Long optionId, String name, String unit, BigDecimal price) {
        RoomServiceOption option = roomServiceOptionRepository.findById(optionId)
            .orElseThrow(() -> new IllegalArgumentException("Layanan tidak ditemukan."));
        if (StringUtils.hasText(name)) {
            option.setName(name.trim());
        } else {
            throw new IllegalArgumentException("Nama layanan wajib diisi.");
        }
        option.setUnit(StringUtils.hasText(unit) ? unit.trim() : "unit");
        BigDecimal safePrice = price != null ? price : BigDecimal.ZERO;
        if (safePrice.signum() < 0) {
            throw new IllegalArgumentException("Harga layanan tidak boleh negatif.");
        }
        option.setPrice(safePrice);
        return roomServiceOptionRepository.save(option);
    }

    @Transactional
    public void deleteServiceOption(Long optionId) {
        RoomServiceOption option = roomServiceOptionRepository.findById(optionId)
            .orElseThrow(() -> new IllegalArgumentException("Layanan tidak ditemukan."));
        roomServiceOptionRepository.delete(option);
    }

    public List<RoomServiceOption> findServiceOptionsForRoom(Long roomId) {
        return roomServiceOptionRepository.findByRoom_IdOrderBySortOrderAscIdAsc(roomId);
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
        request.setMaxOccupancy(room.getMaxOccupancy());
        request.setNote(room.getNote());
        request.setLastCleanedAt(room.getLastCleanedAt());
        request.setAmenitiesText(String.join("\n", resolveFacilityNames(room)));
        request.setImageUrlsText(String.join("\n", resolveImageEntries(room)));
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
        // For view purposes, just return defaults without writing to DB.
        return getDefaultAmenities(room.getType());
    }

    public List<String> resolveImageUrls(Room room) {
        if (room == null) {
            return List.of();
        }
        return Optional.ofNullable(room.getImages()).orElse(List.of()).stream()
            .sorted(Comparator.comparing(RoomImage::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(RoomImage::getId, Comparator.nullsLast(Long::compareTo)))
            .map(RoomImage::getUrl)
            .filter(StringUtils::hasText)
            .map(String::trim)
            .toList();
    }

    public List<String> resolveImageEntries(Room room) {
        if (room == null) {
            return List.of();
        }
        return Optional.ofNullable(room.getImages()).orElse(List.of()).stream()
            .sorted(Comparator.comparing(RoomImage::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(RoomImage::getId, Comparator.nullsLast(Long::compareTo)))
            .map(img -> {
                String url = img.getUrl() != null ? img.getUrl().trim() : "";
                String del = img.getDeleteUrl() != null ? img.getDeleteUrl().trim() : "";
                return StringUtils.hasText(del) ? url + "|" + del : url;
            })
            .filter(StringUtils::hasText)
            .toList();
    }

    public List<Facility> listFacilities() {
        return facilityRepository.findAll(Sort.by(Sort.Order.asc("name").ignoreCase()));
    }

    @Transactional
    public Facility addFacility(String name) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Nama fasilitas tidak boleh kosong.");
        }
        String trimmed = name.trim();
        facilityRepository.findByNameIgnoreCase(trimmed).ifPresent(f -> {
            throw new IllegalArgumentException("Fasilitas sudah ada: " + trimmed);
        });
        Facility f = new Facility();
        f.setName(trimmed);
        return facilityRepository.save(f);
    }

    @Transactional
    public void deleteFacility(Long facilityId) {
        Facility facility = facilityRepository.findById(facilityId)
            .orElseThrow(() -> new IllegalArgumentException("Fasilitas tidak ditemukan."));
        // Bersihkan relasi join terlebih dahulu
        roomFacilityRepository.deleteAll(roomFacilityRepository.findByFacility_Id(facilityId));
        facilityRepository.delete(facility);
    }

    @Transactional
    public List<RoomServiceOption> resolveServiceOptions(Room room) {
        List<RoomServiceOption> stored = Optional.ofNullable(room.getServiceOptions()).orElse(List.of());
        if (!stored.isEmpty()) {
            return sortServices(stored);
        }
        // Persist default options if room already tersimpan supaya bisa dipilih (punya id)
        if (room.getId() != null) {
            List<RoomServiceOption> defaults = defaultServiceOptions(room);
            room.setServiceOptions(defaults);
            Room saved = roomRepository.save(room);
            return sortServices(Optional.ofNullable(saved.getServiceOptions()).orElse(defaults));
        }
        // Jika belum tersimpan, kembalikan default untuk tampilan saja.
        return sortServices(defaultServiceOptions(room));
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
        // Normalisasi literal "\n" menjadi newline agar bisa di-split
        String normalizedRaw = raw.replace("\\n", "\n");
        String[] lines = normalizedRaw.split("\\r?\\n");
        List<String> names = new ArrayList<>();
        for (String line : lines) {
            String safe = sanitizeFacilityName(line);
            if (safe != null) {
                names.add(safe);
            }
        }
        return names;
    }

    private static final class ImageEntry {
        final String url;
        final String deleteUrl;

        ImageEntry(String url, String deleteUrl) {
            this.url = url;
            this.deleteUrl = deleteUrl;
        }
    }

    private List<ImageEntry> parseImageEntries(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        String[] lines = raw.split("\\r?\\n");
        List<ImageEntry> entries = new ArrayList<>();
        for (String line : lines) {
            if (!StringUtils.hasText(line)) {
                continue;
            }
            String trimmed = line.trim();
            String url = trimmed;
            String deleteUrl = null;
            String[] parts = trimmed.split("\\|", 2);
            if (parts.length > 0) {
                url = parts[0].trim();
            }
            if (parts.length > 1 && StringUtils.hasText(parts[1])) {
                deleteUrl = parts[1].trim();
            }
            if (StringUtils.hasText(url)) {
                entries.add(new ImageEntry(url, deleteUrl));
            }
        }
        return entries;
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
        int order = 0;
        defaults.add(option(room, "Laundry", "kg", 25000, order++));
        defaults.add(option(room, "Room dining", "porsi", 60000, order++));
        defaults.add(option(room, "Pembersihan ekstra", "kali", 40000, order++));

        RoomType type = room != null ? room.getType() : null;
        boolean isHighTier = type == RoomType.SUITE_PANORAMA
            || type == RoomType.EXECUTIVE_SUITE
            || type == RoomType.VILLA
            || type == RoomType.PRESIDENTIAL_SUITE;
        boolean isMidTier = !isHighTier && (type == RoomType.DELUXE_KING
            || type == RoomType.DELUXE_TWIN
            || type == RoomType.FAMILY_ROOM
            || type == RoomType.SUPERIOR_TWIN);

        if (isMidTier || isHighTier) {
            defaults.add(option(room, "Camilan Buah", "porsi", 25000, order++));
            defaults.add(option(room, "Pick-up service", "trip", 50000, order++));
            defaults.add(option(room, "Sewa Baby Crib", "unit", 20000, order++));
        }
        if (isHighTier) {
            defaults.add(option(room, "SPA", "sesi", 500000, order++));
            defaults.add(option(room, "Sauna", "sesi", 500000, order++));
            defaults.add(option(room, "Transportasi", "trip", 100000, order++));
            defaults.add(option(room, "Tour", "paket", 350000, order++));
        }
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
        for (String name : Optional.ofNullable(names).orElse(List.of())) {
            if (!StringUtils.hasText(name)) {
                continue;
            }
            String safe = name.trim();
            if (safe.length() > 128) {
                safe = safe.substring(0, 128);
            }
            RoomAmenity amenity = new RoomAmenity();
            amenity.setName(safe);
            amenity.setSortOrder(order++);
            amenity.setRoom(room);
            amenities.add(amenity);
        }
        return amenities;
    }

    private List<RoomImage> toImages(List<ImageEntry> entries, Room room) {
        List<RoomImage> images = new ArrayList<>();
        int order = 0;
        for (ImageEntry entry : Optional.ofNullable(entries).orElse(List.of())) {
            if (entry == null || !StringUtils.hasText(entry.url)) {
                continue;
            }
            RoomImage image = new RoomImage();
            image.setRoom(room);
            image.setUrl(entry.url.trim());
            if (StringUtils.hasText(entry.deleteUrl)) {
                image.setDeleteUrl(entry.deleteUrl.trim());
            } else {
                image.setDeleteUrl(null);
            }
            image.setSortOrder(order++);
            images.add(image);
        }
        return images;
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
            .map(this::sanitizeFacilityName)
            .filter(Objects::nonNull)
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

    private static final String SQL_CREATE_TIPE_KAMAR = """
        CREATE TABLE IF NOT EXISTS tipe_kamar (
          id BIGINT NOT NULL AUTO_INCREMENT,
          code VARCHAR(60) NOT NULL UNIQUE,
          name VARCHAR(120) NOT NULL,
          PRIMARY KEY (id)
        ) ENGINE=InnoDB
        """;

    private static final String SQL_CREATE_KAMAR_TIPE = """
        CREATE TABLE IF NOT EXISTS kamar_tipe (
          id BIGINT NOT NULL AUTO_INCREMENT,
          room_id BIGINT NOT NULL UNIQUE,
          tipe_kamar_id BIGINT NOT NULL,
          PRIMARY KEY (id),
          CONSTRAINT fk_kamar_tipe_room FOREIGN KEY (room_id) REFERENCES rooms(id),
          CONSTRAINT fk_kamar_tipe_type FOREIGN KEY (tipe_kamar_id) REFERENCES tipe_kamar(id)
        ) ENGINE=InnoDB
        """;

    private volatile boolean roomTypeTablesEnsured = false;

    private void syncRoomType(Room room) {
        if (room == null || room.getId() == null || room.getType() == null) {
            return;
        }

        RoomTypeEntity master = roomTypeEntityRepository
            .findByCode(room.getType().name())
            .orElseGet(() -> {
                RoomTypeEntity entity = new RoomTypeEntity();
                entity.setCode(room.getType().name());
                entity.setName(room.getType().getDisplayName());
                return roomTypeEntityRepository.save(entity);
            });

        trySyncRoomTypeJoin(room, master);
    }

    private void trySyncRoomTypeJoin(Room room, RoomTypeEntity master) {
        try {
            upsertRoomTypeJoin(room, master);
        } catch (DataAccessException ex) {
            ensureRoomTypeTables();
            try {
                upsertRoomTypeJoin(room, master);
            } catch (DataAccessException retryEx) {
                throw new IllegalStateException("Gagal menyimpan tipe kamar ke tabel referensi.", retryEx);
            }
        }
    }

    private String sanitizeFacilityName(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        // hilangkan karakter newline/tab dan rapikan spasi
        String normalized = name.replaceAll("[\\r\\n\\t]+", " ").trim();
        // kompres spasi berlebih
        normalized = normalized.replaceAll("\\s{2,}", " ");
        if (normalized.length() > 128) {
            normalized = normalized.substring(0, 128);
        }
        return normalized;
    }

    private void upsertRoomTypeJoin(Room room, RoomTypeEntity master) {
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
    }

    private void ensureRoomTypeTables() {
        if (roomTypeTablesEnsured) {
            return;
        }
        synchronized (this) {
            if (roomTypeTablesEnsured) {
                return;
            }
            try {
                jdbcTemplate.execute(SQL_CREATE_TIPE_KAMAR);
                jdbcTemplate.execute(SQL_CREATE_KAMAR_TIPE);
                roomTypeTablesEnsured = true;
            } catch (DataAccessException ignored) {
                // ignore to allow retry handler to surface a meaningful error
            }
        }
    }

    private static <T> List<T> concat(List<T> a, List<T> b) {
        return new java.util.ArrayList<>() {{
            addAll(a);
            addAll(b);
        }};
    }
}
