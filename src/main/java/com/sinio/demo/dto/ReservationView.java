package com.sinio.demo.dto;

public class ReservationView {
    private Long id;
    private String kode;
    private String nomorKamar;
    private String tipe;
    private String periode;
    private String status;

    public ReservationView(Long id, String kode, String nomorKamar, String tipe, String periode, String status) {
        this.id = id;
        this.kode = kode;
        this.nomorKamar = nomorKamar;
        this.tipe = tipe;
        this.periode = periode;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getKode() {
        return kode;
    }

    public String getNomorKamar() {
        return nomorKamar;
    }

    public String getTipe() {
        return tipe;
    }

    public String getPeriode() {
        return periode;
    }

    public String getStatus() {
        return status;
    }
}

