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
}
