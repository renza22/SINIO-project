package com.sinio.demo.service;

import com.sinio.demo.dto.ReservationRequest;
import com.sinio.demo.dto.ReservationView;
import com.sinio.demo.model.*;
import com.sinio.demo.repository.ReservationRepository;
import com.sinio.demo.repository.ReservationRoomRepository;
import com.sinio.demo.repository.RoomRepository;
import com.sinio.demo.repository.UserRepository;
import com.sinio.demo.repository.StayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationRoomRepository reservationRoomRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final StayRepository stayRepository;

    public ReservationService(
        ReservationRepository reservationRepository,
        ReservationRoomRepository reservationRoomRepository,
        RoomRepository roomRepository,
        UserRepository userRepository,
        StayRepository stayRepository
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationRoomRepository = reservationRoomRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.stayRepository = stayRepository;
    }

    @Transactional
    public Reservation create(Long userId, ReservationRequest request) {
        if (request.getCheckIn().isAfter(request.getCheckOut()) || request.getCheckIn().isEqual(request.getCheckOut())) {
            throw new IllegalArgumentException("Tanggal check-out harus setelah check-in.");
        }
        LocalDate today = LocalDate.now();
        if (request.getCheckIn().isBefore(today)) {
            throw new IllegalArgumentException("Tanggal check-in tidak boleh sebelum hari ini.");
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan."));
        Room room = roomRepository.findById(request.getRoomId()).orElseThrow(() -> new IllegalArgumentException("Kamar tidak ditemukan."));

        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new IllegalArgumentException("Kamar sedang perawatan.");
        }

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setRoom(room); // legacy compatibility
        reservation.setCheckIn(request.getCheckIn());
        reservation.setCheckOut(request.getCheckOut());
        reservation.setStatus(ReservationStatus.BOOKED);
        reservation.setRequestedServices(buildSelectedServices(reservation, room, request.getRequestedServiceIds()));

        ReservationRoom rr = new ReservationRoom();
        rr.setReservation(reservation);
        rr.setRoom(room);
        rr.setNightlyRate(room.getRate());
        reservation.setReservationRooms(List.of(rr));

        Reservation saved = reservationRepository.save(reservation);
        // Set final human-friendly code based on ID
        saved.setCode("RSV-" + saved.getId());
        saved = reservationRepository.save(saved);

        // Mark room as BOOKED (simple approach for demo)
        if (room.getStatus() != RoomStatus.BOOKED) {
            room.setStatus(RoomStatus.BOOKED);
            roomRepository.save(room);
        }

        return saved;
    }

    public List<Reservation> findByUser(Long userId) {
        return reservationRepository.findByUser_IdOrderByCreatedAtDesc(userId);
    }

    public List<ReservationView> toListView(List<Reservation> reservations) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
        return reservations.stream()
            .map(r -> new ReservationView(
                r.getId(),
                r.getCode(),
                primaryRoom(r).getNumber(),
                primaryRoom(r).getType().getDisplayName(),
                fmt.format(r.getCheckIn()) + " - " + fmt.format(r.getCheckOut()),
                r.getStatus().name()
            ))
            .toList();
    }

    @Transactional
    public void cancel(Long userId, Long reservationId) {
        Reservation r = reservationRepository
            .findByIdAndUser_Id(reservationId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Reservasi tidak ditemukan."));
        // Prevent cancel if already checked-in (active stay exists)
        stayRepository.findByReservation_IdAndCheckoutAtIsNull(r.getId()).ifPresent(s -> {
            throw new IllegalStateException("Reservasi sudah check-in dan tidak dapat dibatalkan.");
        });
        List<ReservationRoom> rooms = r.getReservationRooms();
        reservationRepository.delete(r);

        if (rooms == null || rooms.isEmpty()) {
            Room legacy = r.getRoom();
            if (legacy != null) {
                legacy.setStatus(RoomStatus.AVAILABLE);
                roomRepository.save(legacy);
            }
        } else {
            rooms.forEach(rr -> {
            Room room = rr.getRoom();
            if (room != null) {
                room.setStatus(RoomStatus.AVAILABLE);
                roomRepository.save(room);
            }
            });
        }
    }

    public Optional<Reservation> findActiveForUser(Long userId) {
        LocalDate today = LocalDate.now();
        return findByUser(userId).stream()
            .filter(r -> switch (r.getStatus()) {
                case CHECKED_IN, CONFIRMED -> !r.getCheckOut().isBefore(today);
                case BOOKED -> (!r.getCheckIn().isAfter(today) && r.getCheckOut().isAfter(today));
                default -> false;
            })
            .findFirst();
    }

    public java.util.Map<String, Object> toActiveView(Reservation r) {
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("kode", r.getCode());
        m.put("reservasiId", r.getId());
        m.put("checkinRencana", r.getCheckIn());
        m.put("checkoutRencana", r.getCheckOut());
        m.put("nomorKamar", primaryRoom(r).getNumber());
        m.put("namaTipe", primaryRoom(r).getType().getDisplayName());
        m.put("status", r.getStatus().name());
        String badge = switch (r.getStatus()) {
            case BOOKED -> "warning";
            case CHECKED_IN, CONFIRMED -> "success";
            case CHECKED_OUT -> "secondary";
            case CANCELED -> "danger";
        };
        m.put("badge", badge);
        return m;
    }

    // ---- Occupancy helpers for Admin KPI ----
    public Set<Long> getOccupiedRoomIdsToday() {
        return stayRepository.findByCheckoutAtIsNull()
            .stream()
            .map(s -> s.getRoom().getId())
            .collect(Collectors.toSet());
    }

    public long countOccupiedRoomsToday() {
        return getOccupiedRoomIdsToday().size();
    }

    public List<Reservation> findRecent() {
        return reservationRepository.findTop10ByOrderByCreatedAtDesc();
    }

    public List<java.util.Map<String, Object>> recentReservationsView() {
        DateTimeFormatter shortFmt = DateTimeFormatter.ofPattern("dd MMM");
        return findRecent().stream().map(r -> {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("reservasiId", r.getId());
            m.put("kode", r.getCode());
            m.put("tamuNama", r.getUser().getFullName());
            m.put("tipeKamar", primaryRoom(r).getType().getDisplayName());
            m.put("checkinRencana", r.getCheckIn());
            m.put("checkoutRencana", r.getCheckOut());
            m.put("status", r.getStatus().name());
            m.put("servicesSummary", formatServicesSummary(r.getRequestedServices()));
        String badge = switch (r.getStatus()) {
            case BOOKED -> "warning";
            case CHECKED_IN, CONFIRMED -> "success";
            case CHECKED_OUT -> "secondary";
            case CANCELED -> "danger";
        };
            m.put("badge", badge);
            return m;
        }).toList();
    }

    // ---- Front Office views ----
    public List<java.util.Map<String, Object>> arrivalsTodayView() {
        LocalDate today = LocalDate.now();
        List<ReservationStatus> statuses = List.of(ReservationStatus.BOOKED, ReservationStatus.CONFIRMED);
        return reservationRepository
            .findTop10ByStatusInAndCheckInGreaterThanEqualOrderByCheckInAsc(statuses, today)
            .stream()
            .filter(r -> stayRepository.findByReservation_IdAndCheckoutAtIsNull(r.getId()).isEmpty())
            .map(this::toFoRow)
            .toList();
    }

    public List<java.util.Map<String, Object>> departuresTodayView() {
        LocalDate today = LocalDate.now();
        return reservationRepository.findTop10ByCheckOutGreaterThanEqualOrderByCheckOutAsc(today).stream()
            .filter(r -> stayRepository.findByReservation_IdAndCheckoutAtIsNull(r.getId()).isPresent()
                && !r.getCheckOut().isAfter(today))
            .map(this::toFoRow)
            .toList();
    }

    private java.util.Map<String, Object> toFoRow(Reservation r) {
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("reservasiId", r.getId());
        m.put("kode", r.getCode());
        m.put("tamuNama", r.getUser().getFullName());
        m.put("nomorKamar", primaryRoom(r).getNumber());
        m.put("tipeKamar", primaryRoom(r).getType().getDisplayName());
        m.put("checkin", r.getCheckIn());
        m.put("checkout", r.getCheckOut());
        m.put("status", r.getStatus().name());
        m.put("servicesSummary", formatServicesSummary(r.getRequestedServices()));
        return m;
    }

    // All active in-house stays (for early check-out capability)
    public List<java.util.Map<String, Object>> inhouseView() {
        return stayRepository.findByCheckoutAtIsNull()
            .stream()
            .map(stay -> {
                Reservation r = stay.getReservation();
                java.util.Map<String, Object> m = new java.util.HashMap<>();
                m.put("reservasiId", r.getId());
                m.put("kode", r.getCode());
                m.put("tamuNama", r.getUser().getFullName());
                m.put("nomorKamar", stay.getRoom().getNumber());
                m.put("tipeKamar", stay.getRoom().getType().getDisplayName());
                m.put("checkin", r.getCheckIn());
                m.put("checkout", r.getCheckOut()); // planned checkout
                m.put("status", r.getStatus().name());
                m.put("servicesSummary", formatServicesSummary(r.getRequestedServices()));
                return m;
            })
            .toList();
    }

    public List<java.util.Map<String, Object>> serviceOptionsView(Reservation reservation) {
        Room room = primaryRoom(reservation);
        return Optional.ofNullable(room.getServiceOptions()).orElse(List.of()).stream()
            .filter(option -> option.getId() != null)
            .sorted(java.util.Comparator.comparing(RoomServiceOption::getSortOrder, java.util.Comparator.nullsLast(Integer::compareTo))
                .thenComparing(RoomServiceOption::getName, String.CASE_INSENSITIVE_ORDER))
            .map(option -> {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("layananId", option.getId());
                map.put("nama", option.getName());
                map.put("satuan", option.getUnit());
                map.put("hargaSatuan", option.getPrice());
                return map;
            })
            .toList();
    }

    public List<java.util.Map<String, Object>> serviceCartView(Reservation reservation) {
        return Optional.ofNullable(reservation.getRequestedServices()).orElse(List.of()).stream()
            .collect(Collectors.groupingBy(
                s -> s.getName() + "|" + s.getUnit() + "|" + s.getUnitPrice(),
                java.util.LinkedHashMap::new,
                Collectors.toList()
            ))
            .values()
            .stream()
            .map(list -> {
                ReservationServiceSelection first = list.get(0);
                int qty = list.size();
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("nama", first.getName());
                map.put("satuan", first.getUnit());
                map.put("harga", first.getUnitPrice());
                map.put("qty", qty);
                return map;
            })
            .toList();
    }

    @Transactional
    public void addServiceToActiveReservation(Long userId, Long serviceOptionId, int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("Jumlah layanan minimal 1.");
        }
        Reservation reservation = findActiveForUser(userId)
            .orElseThrow(() -> new IllegalStateException("Tidak ada reservasi aktif."));
        if (reservation.getStatus() == ReservationStatus.CANCELED || reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
            throw new IllegalStateException("Reservasi tidak aktif.");
        }

        Room room = primaryRoom(reservation);
        RoomServiceOption option = Optional.ofNullable(room.getServiceOptions()).orElse(List.of()).stream()
            .filter(o -> o.getId() != null && o.getId().equals(serviceOptionId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Layanan tidak ditemukan untuk kamar ini."));

        if (reservation.getRequestedServices() == null) {
            reservation.setRequestedServices(new ArrayList<>());
        }
        for (int i = 0; i < qty; i++) {
            ReservationServiceSelection selection = ReservationServiceSelection.of(
                option.getName(),
                option.getUnit(),
                option.getPrice()
            );
            selection.setReservation(reservation);
            reservation.getRequestedServices().add(selection);
        }
        reservationRepository.save(reservation);
    }

    

    // ---- Guest-driven transitions with ownership checks ----
    @Transactional
    public void guestCheckIn(Long userId, Long reservationId) {
        throw new IllegalStateException("Check-in hanya dapat dilakukan oleh Karyawan di front desk.");
    }

    @Transactional
    public void guestCheckOut(Long userId, Long reservationId) {
        throw new IllegalStateException("Check-out hanya dapat dilakukan oleh Karyawan di front desk.");
    }

    // ---- Staff (Front Desk) operations ----
    @Transactional
    public void staffCheckIn(Long reservationId) {
        Reservation r = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("Reservasi tidak ditemukan."));
        if (r.getStatus() != ReservationStatus.BOOKED) {
            // allow reconfirm or already confirmed?
            if (r.getStatus() != ReservationStatus.CONFIRMED) {
                throw new IllegalStateException("Reservasi tidak dalam status BOOKED/CONFIRMED.");
            }
        }
        // ensure stay not exists
        stayRepository.findByReservation_IdAndCheckoutAtIsNull(r.getId()).ifPresent(s -> {
            throw new IllegalStateException("Tamu sudah check-in.");
        });

        r.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(r);

        Stay stay = new Stay();
        stay.setUser(r.getUser());
        stay.setReservation(r);
        stay.setRoom(primaryRoom(r));
        stay.setCheckinAt(java.time.LocalDateTime.now());
        stayRepository.save(stay);

        Room room = primaryRoom(r);
        if (room != null) {
            room.setStatus(RoomStatus.OCCUPIED);
            roomRepository.save(room);
        }
    }

    @Transactional
    public void staffCheckOut(Long reservationId) {
        Reservation r = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("Reservasi tidak ditemukan."));
        Stay stay = stayRepository.findByReservation_IdAndCheckoutAtIsNull(r.getId())
            .orElseThrow(() -> new IllegalStateException("Belum ada data menginap aktif."));

        stay.setCheckoutAt(java.time.LocalDateTime.now());
        stayRepository.save(stay);

        r.setStatus(ReservationStatus.CHECKED_OUT);
        reservationRepository.save(r);

        Room room = primaryRoom(r);
        if (room != null) {
            room.setStatus(RoomStatus.CLEANING);
            roomRepository.save(room);
        }
    }

    private List<ReservationServiceSelection> buildSelectedServices(Reservation reservation, Room room, List<Long> requestedIds) {
        if (room == null || requestedIds == null || requestedIds.isEmpty()) {
            return List.of();
        }
        Map<Long, RoomServiceOption> optionMap = Optional.ofNullable(room.getServiceOptions()).orElse(List.of()).stream()
            .filter(option -> option.getId() != null)
            .collect(Collectors.toMap(RoomServiceOption::getId, option -> option, (left, right) -> left));

        LinkedHashSet<Long> uniqueIds = requestedIds.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        List<ReservationServiceSelection> selections = new ArrayList<>();
        for (Long id : uniqueIds) {
            RoomServiceOption option = optionMap.get(id);
            if (option != null) {
                ReservationServiceSelection selection = ReservationServiceSelection.of(
                    option.getName(),
                    option.getUnit(),
                    option.getPrice()
                );
                selection.setReservation(reservation);
                selections.add(selection);
            }
        }
        return selections;
    }

    private String formatServicesSummary(List<ReservationServiceSelection> selections) {
        if (selections == null || selections.isEmpty()) {
            return "-";
        }
        return selections.stream()
            .map(s -> s.getName())
            .filter(Objects::nonNull)
            .collect(Collectors.joining(", "));
    }

    private Room primaryRoom(Reservation reservation) {
        if (reservation == null) {
            throw new IllegalArgumentException("Reservasi tidak ditemukan.");
        }
        Room room = reservation.primaryRoom();
        if (room == null) {
            throw new IllegalStateException("Reservasi belum memiliki kamar terasosiasi.");
        }
        return room;
    }
}
