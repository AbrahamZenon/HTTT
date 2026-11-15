package com.longchau.model;

/**
 * Chi tiết hóa đơn: Gắn với Hóa đơn, Thuốc và Mã lô cụ thể để hỗ trợ trừ tồn kho (BR10)
 */
public class ChiTietHoaDon {
    private int id;
    private String maHD;
    private String maThuoc;
    private String maLo;
    private int soLuong;
    private double donGia;

    // Thuộc tính mở rộng tiện cho hiển thị trên giao diện/báo cáo
    private String tenThuoc;
    private String donViTinh;
    private String hanSuDung;

    public ChiTietHoaDon() {}

    public ChiTietHoaDon(int id, String maHD, String maThuoc, String maLo, int soLuong, double donGia) {
        this.id = id;
        this.maHD = maHD;
        this.maThuoc = maThuoc;
        this.maLo = maLo;
        this.soLuong = soLuong;
        this.donGia = donGia;
    }

    public double getThanhTien() {
        return this.soLuong * this.donGia;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMaHD() { return maHD; }
    public void setMaHD(String maHD) { this.maHD = maHD; }

    public String getMaThuoc() { return maThuoc; }
    public void setMaThuoc(String maThuoc) { this.maThuoc = maThuoc; }

    public String getMaLo() { return maLo; }
    public void setMaLo(String maLo) { this.maLo = maLo; }

    public int getSoLuong() { return soLuong; }
    public void setSoLuong(int soLuong) { this.soLuong = soLuong; }

    public double getDonGia() { return donGia; }
    public void setDonGia(double donGia) { this.donGia = donGia; }

    public String getTenThuoc() { return tenThuoc; }
    public void setTenThuoc(String tenThuoc) { this.tenThuoc = tenThuoc; }

    public String getDonViTinh() { return donViTinh; }
    public void setDonViTinh(String donViTinh) { this.donViTinh = donViTinh; }

    public String getHanSuDung() { return hanSuDung; }
    public void setHanSuDung(String hanSuDung) { this.hanSuDung = hanSuDung; }
}

