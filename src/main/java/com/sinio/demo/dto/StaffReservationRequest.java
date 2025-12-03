package com.sinio.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StaffReservationRequest {

    @NotBlank(message = "Nama tamu wajib diisi.")
    private String fullName;

    @NotBlank(message = "Email tamu wajib diisi.")
    @Email(message = "Email tamu tidak valid.")
    private String email;

    @NotNull(message = "Kamar wajib dipilih.")
    private Long roomId;

    @NotNull(message = "Tanggal check-in wajib diisi.")
    @FutureOrPresent(message = "Tanggal check-in tidak boleh sebelum hari ini.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkIn;

    @NotNull(message = "Tanggal check-out wajib diisi.")
    @Future(message = "Tanggal check-out harus di masa depan.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkOut;

    private List<Long> requestedServiceIds = new ArrayList<>();

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(LocalDate checkIn) {
        this.checkIn = checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(LocalDate checkOut) {
        this.checkOut = checkOut;
    }

    public List<Long> getRequestedServiceIds() {
        return requestedServiceIds;
    }

    public void setRequestedServiceIds(List<Long> requestedServiceIds) {
        this.requestedServiceIds = requestedServiceIds != null ? requestedServiceIds : new ArrayList<>();
    }
}
