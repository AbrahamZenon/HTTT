package com.longchau.model;

import java.time.LocalDate;

public final class ChiTietPhieuNhap {
    private final String maThuoc;
    private final String maLo;
    private final LocalDate hanSuDung;
    private final int soLuongNhap;
    private final long donGiaNhap;

    public ChiTietPhieuNhap(String maThuoc, String maLo, LocalDate hanSuDung, int soLuongNhap, long donGiaNhap) {
        this.maThuoc = maThuoc;
        this.maLo = maLo == null ? "" : maLo.trim().toUpperCase(java.util.Locale.ROOT);
        this.hanSuDung = hanSuDung;
        this.soLuongNhap = soLuongNhap;
        this.donGiaNhap = donGiaNhap;
    }

    public String getMaThuoc() { return maThuoc; }
    public String getMaLo() { return maLo; }
    public LocalDate getHanSuDung() { return hanSuDung; }
    public int getSoLuongNhap() { return soLuongNhap; }
    public long getDonGiaNhap() { return donGiaNhap; }
    public String getKhoaLo() { return maLo + "#" + maThuoc; }
}
