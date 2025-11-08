package com.sinio.demo.service;

import com.sinio.demo.dto.ReservationRequest;
import com.sinio.demo.model.*;
import com.sinio.demo.repository.ReservationRepository;
import com.sinio.demo.repository.RoomRepository;
import com.sinio.demo.repository.UserRepository;
import com.sinio.demo.repository.StayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.sinio.demo.dto.ReservationView;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final StayRepository stayRepository;

    public ReservationService(ReservationRepository reservationRepository, RoomRepository roomRepository, UserRepository userRepository, StayRepository stayRepository) {
        this.reservationRepository = reservationRepository;
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
        reservation.setRoom(room);
        reservation.setCheckIn(request.getCheckIn());
        reservation.setCheckOut(request.getCheckOut());
        reservation.setStatus(ReservationStatus.BOOKED);
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
                r.getRoom().getNumber(),
                r.getRoom().getType().getDisplayName(),
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
        Room room = r.getRoom();
        reservationRepository.delete(r);

        if (room != null) {
            // simple approach: kembalikan status kamar ke AVAILABLE
            room.setStatus(RoomStatus.AVAILABLE);
            roomRepository.save(room);
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
        m.put("nomorKamar", r.getRoom().getNumber());
        m.put("namaTipe", r.getRoom().getType().getDisplayName());
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
            m.put("tipeKamar", r.getRoom().getType().getDisplayName());
            m.put("checkinRencana", r.getCheckIn());
            m.put("checkoutRencana", r.getCheckOut());
            m.put("status", r.getStatus().name());
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
        return reservationRepository.findByCheckInEquals(today).stream()
            .filter(r -> r.getStatus() == ReservationStatus.BOOKED || r.getStatus() == ReservationStatus.CONFIRMED)
            .map(this::toFoRow)
            .toList();
    }

    public List<java.util.Map<String, Object>> departuresTodayView() {
        LocalDate today = LocalDate.now();
        return reservationRepository.findByCheckOutEquals(today).stream()
            .filter(r -> stayRepository.findByReservation_IdAndCheckoutAtIsNull(r.getId()).isPresent())
            .map(this::toFoRow)
            .toList();
    }

    private java.util.Map<String, Object> toFoRow(Reservation r) {
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("reservasiId", r.getId());
        m.put("kode", r.getCode());
        m.put("tamuNama", r.getUser().getFullName());
        m.put("nomorKamar", r.getRoom().getNumber());
        m.put("tipeKamar", r.getRoom().getType().getDisplayName());
        m.put("checkin", r.getCheckIn());
        m.put("checkout", r.getCheckOut());
        m.put("status", r.getStatus().name());
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
                return m;
            })
            .toList();
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
        LocalDate today = LocalDate.now();
        if (r.getStatus() != ReservationStatus.BOOKED) {
            // allow reconfirm or already confirmed?
            if (r.getStatus() != ReservationStatus.CONFIRMED) {
                throw new IllegalStateException("Reservasi tidak dalam status BOOKED/CONFIRMED.");
            }
        }
        if (r.getCheckIn().isAfter(today)) {
            throw new IllegalStateException("Belum masuk tanggal check-in.");
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
        stay.setRoom(r.getRoom());
        stay.setCheckinAt(java.time.LocalDateTime.now());
        stayRepository.save(stay);

        Room room = r.getRoom();
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

        Room room = r.getRoom();
        if (room != null) {
            room.setStatus(RoomStatus.CLEANING);
            roomRepository.save(room);
        }
    }
}
