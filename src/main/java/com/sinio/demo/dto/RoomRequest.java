package com.sinio.demo.dto;

import com.sinio.demo.model.RoomStatus;
import com.sinio.demo.model.RoomType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RoomRequest {

    private Long id;

    @NotBlank(message = "Nomor kamar wajib diisi.")
    @Size(max = 16, message = "Nomor kamar maksimal 16 karakter.")
    private String number;

    @NotNull(message = "Tipe kamar wajib dipilih.")
    private RoomType type;

    @NotNull(message = "Tarif kamar wajib diisi.")
    @DecimalMin(value = "0.0", inclusive = false, message = "Tarif harus lebih besar dari 0.")
    @Digits(integer = 10, fraction = 2, message = "Format tarif tidak valid.")
    private BigDecimal rate;

    @NotNull(message = "Status kamar wajib dipilih.")
    private RoomStatus status;

    @NotNull(message = "Kapasitas kamar wajib diisi.")
    @Min(value = 1, message = "Kapasitas minimal 1 orang.")
    @Max(value = 10, message = "Kapasitas maksimal 10 orang per kamar.")
    private Integer maxOccupancy;

    @Size(max = 2000, message = "Catatan maksimal 2000 karakter.")
    private String note;

    private LocalDateTime lastCleanedAt;

    @Size(max = 2000, message = "Daftar fasilitas maksimal 2000 karakter.")
    private String amenitiesText;

    @Size(max = 4000, message = "Daftar layanan maksimal 4000 karakter.")
    private String servicesText;

    @Size(max = 4000, message = "Daftar foto maksimal 4000 karakter.")
    private String imageUrlsText;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public RoomType getType() {
        return type;
    }

    public void setType(RoomType type) {
        this.type = type;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public Integer getMaxOccupancy() {
        return maxOccupancy;
    }

    public void setMaxOccupancy(Integer maxOccupancy) {
        this.maxOccupancy = maxOccupancy;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getLastCleanedAt() {
        return lastCleanedAt;
    }

    public void setLastCleanedAt(LocalDateTime lastCleanedAt) {
        this.lastCleanedAt = lastCleanedAt;
    }

    public String getAmenitiesText() {
        return amenitiesText;
    }

    public void setAmenitiesText(String amenitiesText) {
        this.amenitiesText = amenitiesText;
    }

    public String getServicesText() {
        return servicesText;
    }

    public void setServicesText(String servicesText) {
        this.servicesText = servicesText;
    }

    public String getImageUrlsText() {
        return imageUrlsText;
    }

    public void setImageUrlsText(String imageUrlsText) {
        this.imageUrlsText = imageUrlsText;
    }
}
