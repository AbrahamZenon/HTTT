package com.longchau.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Thực thể Hóa đơn bán hàng
 */
public class HoaDon {
    private String maHD;
    private String ngayLap;
    private double tongTien;
    private TrangThaiHoaDon trangThai;
    private String maNV;
    private String ghiChu;
    private List<ChiTietHoaDon> chiTiet = new ArrayList<>();
    private ThanhToan thanhToan;

    public HoaDon() {}

    public HoaDon(String maHD, String ngayLap, double tongTien, TrangThaiHoaDon trangThai, String maNV, String ghiChu) {
        this.maHD = maHD;
        this.ngayLap = ngayLap;
        this.tongTien = tongTien;
        this.trangThai = trangThai;
        this.maNV = maNV;
        this.ghiChu = ghiChu;
    }

    public String getMaHD() { return maHD; }
    public void setMaHD(String maHD) { this.maHD = maHD; }

    public String getNgayLap() { return ngayLap; }
    public void setNgayLap(String ngayLap) { this.ngayLap = ngayLap; }

    public double getTongTien() { return tongTien; }
    public void setTongTien(double tongTien) { this.tongTien = tongTien; }

    public TrangThaiHoaDon getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiHoaDon trangThai) { this.trangThai = trangThai; }

    public String getMaNV() { return maNV; }
    public void setMaNV(String maNV) { this.maNV = maNV; }

    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }

    public List<ChiTietHoaDon> getChiTiet() { return chiTiet; }
    public void setChiTiet(List<ChiTietHoaDon> chiTiet) { this.chiTiet = chiTiet; }

    public ThanhToan getThanhToan() { return thanhToan; }
    public void setThanhToan(ThanhToan thanhToan) { this.thanhToan = thanhToan; }
}

