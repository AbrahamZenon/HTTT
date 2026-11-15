package com.longchau.model;

/**
 * Thực thể Nhân viên hệ thống
 */
public class NhanVien {
    private String maNV;
    private String hoTen;
    private String vaiTro; // BAN_HANG, NHAP_HANG, QUAN_LY
    private String tenDangNhap;

    public NhanVien() {}

    public NhanVien(String maNV, String hoTen, String vaiTro, String tenDangNhap) {
        this.maNV = maNV;
        this.hoTen = hoTen;
        this.vaiTro = vaiTro;
        this.tenDangNhap = tenDangNhap;
    }

    public String getMaNV() { return maNV; }
    public void setMaNV(String maNV) { this.maNV = maNV; }

    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }

    public String getVaiTro() { return vaiTro; }
    public void setVaiTro(String vaiTro) { this.vaiTro = vaiTro; }

    public String getTenDangNhap() { return tenDangNhap; }
    public void setTenDangNhap(String tenDangNhap) { this.tenDangNhap = tenDangNhap; }
}

