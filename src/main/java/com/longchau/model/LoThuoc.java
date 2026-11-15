package com.longchau.model;

/**
 * Thực thể Lô thuốc: Quản lý mã lô, hạn dùng và số lượng tồn theo lô
 */
public class LoThuoc {
    private String maLo;
    private String maThuoc;
    private String hanSuDung; // YYYY-MM-DD
    private int soLuongTon;

    public LoThuoc() {}

    public LoThuoc(String maLo, String maThuoc, String hanSuDung, int soLuongTon) {
        this.maLo = maLo;
        this.maThuoc = maThuoc;
        this.hanSuDung = hanSuDung;
        this.soLuongTon = soLuongTon;
    }

    public String getMaLo() { return maLo; }
    public void setMaLo(String maLo) { this.maLo = maLo; }

    public String getMaThuoc() { return maThuoc; }
    public void setMaThuoc(String maThuoc) { this.maThuoc = maThuoc; }

    public String getHanSuDung() { return hanSuDung; }
    public void setHanSuDung(String hanSuDung) { this.hanSuDung = hanSuDung; }

    public int getSoLuongTon() { return soLuongTon; }
    public void setSoLuongTon(int soLuongTon) { this.soLuongTon = soLuongTon; }
}
