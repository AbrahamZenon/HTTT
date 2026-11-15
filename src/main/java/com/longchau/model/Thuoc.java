package com.longchau.model;

/**
 * Thực thể Thuốc
 */
public class Thuoc {
    private String maThuoc;
    private String tenThuoc;
    private String donViTinh;
    private double giaBan;
    private String trangThai; // DANG_KINH_DOAN, NGUNG_KINH_DOAN

    public Thuoc() {}

    public Thuoc(String maThuoc, String tenThuoc, String donViTinh, double giaBan, String trangThai) {
        this.maThuoc = maThuoc;
        this.tenThuoc = tenThuoc;
        this.donViTinh = donViTinh;
        this.giaBan = giaBan;
        this.trangThai = trangThai;
    }

    public String getMaThuoc() { return maThuoc; }
    public void setMaThuoc(String maThuoc) { this.maThuoc = maThuoc; }

    public String getTenThuoc() { return tenThuoc; }
    public void setTenThuoc(String tenThuoc) { this.tenThuoc = tenThuoc; }

    public String getDonViTinh() { return donViTinh; }
    public void setDonViTinh(String donViTinh) { this.donViTinh = donViTinh; }

    public double getGiaBan() { return giaBan; }
    public void setGiaBan(double giaBan) { this.giaBan = giaBan; }

    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
}

