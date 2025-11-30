package com.sinio.demo.service;

import com.sinio.demo.dto.ReservationRequest;
import com.sinio.demo.dto.ReservationView;
import com.sinio.demo.model.*;
import com.sinio.demo.repository.ReservationRepository;
import com.sinio.demo.repository.ReservationRoomRepository;
import com.sinio.demo.repository.RoomRepository;
import com.sinio.demo.repository.UserRepository;
import com.sinio.demo.repository.StayRepository;
import com.sinio.demo.repository.PaymentRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final PaymentRepository paymentRepository;
    private final JdbcTemplate jdbcTemplate;

    public ReservationService(
        ReservationRepository reservationRepository,
        ReservationRoomRepository reservationRoomRepository,
        RoomRepository roomRepository,
        UserRepository userRepository,
        StayRepository stayRepository,
        PaymentRepository paymentRepository,
        JdbcTemplate jdbcTemplate
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationRoomRepository = reservationRoomRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.stayRepository = stayRepository;
        this.paymentRepository = paymentRepository;
        this.jdbcTemplate = jdbcTemplate;
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
        reservation.setStatus(ReservationStatus.PENDING_PAYMENT);
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

        return saved;
    }

    public List<Reservation> findByUser(Long userId) {
        try {
            return reservationRepository.findByUser_IdOrderByCreatedAtDesc(userId);
        } catch (org.springframework.orm.jpa.JpaObjectRetrievalFailureException | jakarta.persistence.EntityNotFoundException ex) {
            purgeOrphanReservations();
            try {
                return reservationRepository.findByUser_IdOrderByCreatedAtDesc(userId);
            } catch (Exception retryEx) {
                // jika data tetap kotor, jangan blokir akses tamu
                return List.of();
            }
        }
    }

    public List<ReservationView> toListView(List<Reservation> reservations) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
        return reservations.stream()
            .map(r -> {
                Room room = safePrimaryRoom(r);
                if (room == null) {
                    return null;
                }
                return new ReservationView(
                    r.getId(),
                    r.getCode(),
                    room.getNumber(),
                    room.getType().getDisplayName(),
                    fmt.format(r.getCheckIn()) + " - " + fmt.format(r.getCheckOut()),
                    r.getStatus().name()
                );
            })
            .filter(java.util.Objects::nonNull)
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
        deleteAndRelease(r);
    }

    public Optional<Reservation> findActiveForUser(Long userId) {
        LocalDate today = LocalDate.now();
        return findByUser(userId).stream()
            .filter(r -> switch (r.getStatus()) {
                case PENDING_PAYMENT -> hasPendingCashPayment(r) && !r.getCheckOut().isBefore(today);
                case CHECKED_IN, CONFIRMED -> !r.getCheckOut().isBefore(today);
                case BOOKED -> (!r.getCheckIn().isAfter(today) && r.getCheckOut().isAfter(today));
                default -> false;
            })
            .filter(r -> safePrimaryRoom(r) != null)
            .findFirst();
    }

    public java.util.Map<String, Object> toActiveView(Reservation r) {
        Room room = safePrimaryRoom(r);
        if (room == null) {
            return null; // data tidak konsisten, jangan blokir halaman
        }
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("kode", r.getCode());
        m.put("reservasiId", r.getId());
        m.put("checkinRencana", r.getCheckIn());
        m.put("checkoutRencana", r.getCheckOut());
        m.put("nomorKamar", room.getNumber());
        m.put("namaTipe", room.getType().getDisplayName());
        m.put("hargaPerMalam", room.getRate());
        m.put("status", r.getStatus().name());
        String badge = switch (r.getStatus()) {
            case PENDING_PAYMENT -> "secondary";
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
            .map(s -> s.getRoom())
            .filter(Objects::nonNull)
            .map(Room::getId)
            .collect(Collectors.toSet());
    }

    public long countOccupiedRoomsToday() {
        return getOccupiedRoomIdsToday().size();
    }

    public List<Reservation> findRecent() {
        try {
            return reservationRepository.findTop10ByOrderByCreatedAtDesc();
        } catch (org.springframework.orm.jpa.JpaObjectRetrievalFailureException | jakarta.persistence.EntityNotFoundException ex) {
            purgeOrphanReservations();
            try {
                return reservationRepository.findTop10ByOrderByCreatedAtDesc();
            } catch (Exception retryEx) {
                // jika data masih kotor setelah purge, kosongkan supaya dashboard tetap jalan
                return List.of();
            }
        } catch (RuntimeException ex) {
            purgeOrphanReservations();
            try {
                return reservationRepository.findTop10ByOrderByCreatedAtDesc();
            } catch (Exception retryEx) {
                // jika tetap gagal, kembalikan list kosong supaya dashboard tetap bisa render
                return List.of();
            }
        }
    }

    public Optional<Reservation> findById(Long id) {
        return reservationRepository.findById(id);
    }

    public List<java.util.Map<String, Object>> recentReservationsView() {
        DateTimeFormatter shortFmt = DateTimeFormatter.ofPattern("dd MMM");
        return findRecent().stream()
            .map(r -> {
                Room room = safePrimaryRoom(r);
                if (room == null) {
                    return null;
                }
                java.util.Map<String, Object> m = new java.util.HashMap<>();
                m.put("reservasiId", r.getId());
                m.put("kode", r.getCode());
                m.put("tamuNama", r.getUser().getFullName());
                m.put("nomorKamar", room.getNumber());
                m.put("tipeKamar", room.getType().getDisplayName());
                m.put("checkinRencana", r.getCheckIn());
                m.put("checkoutRencana", r.getCheckOut());
                m.put("status", r.getStatus().name());
                m.put("servicesSummary", formatServicesSummary(r.getRequestedServices()));
            String badge = switch (r.getStatus()) {
                case PENDING_PAYMENT -> "secondary";
                case BOOKED -> "warning";
                case CHECKED_IN, CONFIRMED -> "success";
                case CHECKED_OUT -> "secondary";
                case CANCELED -> "danger";
            };
                m.put("badge", badge);
                return m;
            })
            .filter(java.util.Objects::nonNull)
            .toList();
    }

    // ---- Front Office views ----
    public List<java.util.Map<String, Object>> arrivalsTodayView() {
        purgeOrphanReservations();
        List<ReservationStatus> statuses = List.of(ReservationStatus.BOOKED, ReservationStatus.CONFIRMED);
        // tampilkan 20 teratas tanpa membatasi tanggal supaya reservasi yang tertunda tetap bisa di-approve
        return reservationRepository.findTop20ByStatusInOrderByCheckInAsc(statuses).stream()
            .filter(r -> stayRepository.findByReservation_IdAndCheckoutAtIsNull(r.getId()).isEmpty())
            .map(this::toFoRow)
            .filter(Objects::nonNull)
            .toList();
    }

    public List<java.util.Map<String, Object>> departuresTodayView() {
        purgeOrphanReservations();
        LocalDate today = LocalDate.now();
        return reservationRepository.findTop10ByCheckOutGreaterThanEqualOrderByCheckOutAsc(today).stream()
            .filter(r -> stayRepository.findByReservation_IdAndCheckoutAtIsNull(r.getId()).isPresent()
                && !r.getCheckOut().isAfter(today))
            .map(this::toFoRow)
            .filter(Objects::nonNull)
            .toList();
    }

    private java.util.Map<String, Object> toFoRow(Reservation r) {
        Room room = safePrimaryRoom(r);
        if (room == null) {
            return null; // skip reservation with missing/invalid room linkage
        }
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("reservasiId", r.getId());
        m.put("kode", r.getCode());
        m.put("tamuNama", r.getUser().getFullName());
        m.put("nomorKamar", room.getNumber());
        m.put("tipeKamar", room.getType().getDisplayName());
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
        Room room = safePrimaryRoom(reservation);
        if (room == null) {
            return List.of();
        }
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

        Room room = safePrimaryRoom(reservation);
        if (room == null) {
            throw new IllegalStateException("Kamar untuk reservasi ini sudah tidak tersedia.");
        }
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

    @Transactional
    public void confirmPayment(Reservation reservation) {
        if (reservation == null) {
            return;
        }
        reservation.setStatus(ReservationStatus.BOOKED);
        reservationRepository.save(reservation);

        Room room = safePrimaryRoom(reservation);
        if (room != null && room.getStatus() != RoomStatus.BOOKED) {
            room.setStatus(RoomStatus.BOOKED);
            roomRepository.save(room);
        }
    }

    /**
     * Hapus reservasi yang belum dibayar dan kembalikan status kamar.
     */
    @Transactional
    public void deleteAndRelease(Reservation reservation) {
        if (reservation == null) {
            return;
        }
        if (reservation.getId() != null) {
            List<Payment> payments = paymentRepository.findByReservationId(reservation.getId());
            boolean hasSuccess = payments != null && payments.stream().anyMatch(p -> p.getStatus() == PaymentStatus.SUCCESS);
            if (hasSuccess) {
                throw new IllegalStateException("Reservasi sudah memiliki pembayaran yang berhasil sehingga tidak dapat dihapus otomatis.");
            }
            if (payments != null && !payments.isEmpty()) {
                paymentRepository.deleteAll(payments);
            }
        }
        List<ReservationRoom> rooms = reservation.getReservationRooms();
        if (rooms == null || rooms.isEmpty()) {
            Room legacy = reservation.getRoom();
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
        reservationRepository.delete(reservation);
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

        Room room = safePrimaryRoom(r);
        if (room == null) {
            throw new IllegalStateException("Kamar untuk reservasi ini sudah dihapus.");
        }

        Stay stay = new Stay();
        stay.setUser(r.getUser());
        stay.setReservation(r);
        stay.setRoom(room);
        stay.setCheckinAt(java.time.LocalDateTime.now());
        stayRepository.save(stay);

        room.setStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room);
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

        Room room = safePrimaryRoom(r);
        if (room == null) {
            throw new IllegalStateException("Kamar untuk reservasi ini sudah dihapus.");
        }
        room.setStatus(RoomStatus.CLEANING);
        roomRepository.save(room);
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

    public Room primaryRoom(Reservation reservation) {
        if (reservation == null) {
            throw new IllegalArgumentException("Reservasi tidak ditemukan.");
        }
        Room room = reservation.primaryRoom();
        if (room == null) {
            throw new IllegalStateException("Reservasi belum memiliki kamar terasosiasi.");
        }
        return room;
    }

    public Room safePrimaryRoom(Reservation reservation) {
        try {
            return primaryRoom(reservation);
        } catch (IllegalStateException | org.springframework.orm.jpa.JpaObjectRetrievalFailureException | jakarta.persistence.EntityNotFoundException ex) {
            // Skip data yang sudah tidak konsisten (mis. kamar dihapus)
            return null;
        }
    }

    private boolean hasPendingCashPayment(Reservation reservation) {
        return paymentRepository.findTop1ByReservationIdOrderByCreatedAtDesc(reservation.getId())
            .map(p -> p.getStatus() == PaymentStatus.PENDING
                && p.getPaymentType() != null
                && "CASH".equalsIgnoreCase(p.getPaymentType()))
            .orElse(false);
    }

    private void purgeOrphanReservations() {
        try {
            jdbcTemplate.update("DELETE FROM reservation_rooms WHERE room_id NOT IN (SELECT id FROM rooms)");
            jdbcTemplate.update("DELETE FROM stays WHERE room_id NOT IN (SELECT id FROM rooms)");
            jdbcTemplate.update("DELETE FROM reservations WHERE room_id NOT IN (SELECT id FROM rooms)");
        } catch (DataAccessException ignored) {
            // jika gagal, biarkan supaya error asli tetap terlihat
        }
    }
}
