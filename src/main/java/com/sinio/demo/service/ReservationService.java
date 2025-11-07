package com.sinio.demo.service;

import com.sinio.demo.dto.ReservationRequest;
import com.sinio.demo.model.*;
import com.sinio.demo.repository.ReservationRepository;
import com.sinio.demo.repository.RoomRepository;
import com.sinio.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.sinio.demo.dto.ReservationView;
import java.util.Optional;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public ReservationService(ReservationRepository reservationRepository, RoomRepository roomRepository, UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
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
                case CHECKED_IN -> !r.getCheckOut().isBefore(today);
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
            case CHECKED_IN -> "success";
            case CHECKED_OUT -> "secondary";
            case CANCELED -> "danger";
        };
        m.put("badge", badge);
        return m;
    }
}
