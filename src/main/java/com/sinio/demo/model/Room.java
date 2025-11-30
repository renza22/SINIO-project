package com.sinio.demo.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 16)
    private String number;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RoomType type;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal rate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomStatus status;

    @Column(nullable = false)
    private Integer maxOccupancy;

    @Column(columnDefinition = "TEXT")
    private String note;

    private LocalDateTime lastCleanedAt;

    @OneToMany(
        mappedBy = "room",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @OrderBy("sortOrder ASC, id ASC")
    private List<RoomAmenity> amenities = new ArrayList<>();

    @OneToMany(
        mappedBy = "room",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @OrderBy("sortOrder ASC, id ASC")
    private List<RoomServiceOption> serviceOptions = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (maxOccupancy == null || maxOccupancy < 1) {
            maxOccupancy = 2;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (maxOccupancy == null || maxOccupancy < 1) {
            maxOccupancy = 2;
        }
    }

    public Long getId() {
        return id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<RoomAmenity> getAmenities() {
        return amenities;
    }

    public void setAmenities(List<RoomAmenity> amenities) {
        this.amenities.clear();
        if (amenities == null) {
            return;
        }
        for (int i = 0; i < amenities.size(); i++) {
            RoomAmenity amenity = amenities.get(i);
            amenity.setRoom(this);
            if (amenity.getSortOrder() == null) {
                amenity.setSortOrder(i);
            }
            this.amenities.add(amenity);
        }
    }

    public List<RoomServiceOption> getServiceOptions() {
        return serviceOptions;
    }

    public void setServiceOptions(List<RoomServiceOption> serviceOptions) {
        this.serviceOptions.clear();
        if (serviceOptions == null) {
            return;
        }
        for (int i = 0; i < serviceOptions.size(); i++) {
            RoomServiceOption option = serviceOptions.get(i);
            option.setRoom(this);
            if (option.getSortOrder() == null) {
                option.setSortOrder(i);
            }
            this.serviceOptions.add(option);
        }
    }
}
