package com.sinio.demo.dto;

import com.sinio.demo.model.RoomStatus;
import com.sinio.demo.model.RoomType;

import java.math.BigDecimal;

/**
 * Lightweight projection for guest-facing room list.
 */
public class RoomSummaryView {

    private final Long id;
    private final String number;
    private final RoomType type;
    private final RoomStatus status;
    private final BigDecimal rate;

    public RoomSummaryView(Long id, String number, RoomType type, RoomStatus status, BigDecimal rate) {
        this.id = id;
        this.number = number;
        this.type = type;
        this.status = status;
        this.rate = rate;
    }

    public Long getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public RoomType getType() {
        return type;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public String getTypeDisplayName() {
        return type != null ? type.getDisplayName() : "";
    }

    public String getStatusDisplayName() {
        return status != null ? status.getDisplayName() : "";
    }
}
