package com.longchau.model;

/**
 * Thực thể Thanh toán (Gắn với Hóa đơn theo BR05)
 */
public class ThanhToan {
    private String maTT;
    private String maHD;
    private double soTien;
    private PhuongThucThanhToan phuongThuc;
    private TrangThaiThanhToan trangThai;
    private String ngayTao;
    private String maGiaoDich;
    private String ghiChu;

    public ThanhToan() {}

    public ThanhToan(String maTT, String maHD, double soTien, PhuongThucThanhToan phuongThuc,
                     TrangThaiThanhToan trangThai, String ngayTao, String maGiaoDich, String ghiChu) {
        this.maTT = maTT;
        this.maHD = maHD;
        this.soTien = soTien;
        this.phuongThuc = phuongThuc;
        this.trangThai = trangThai;
        this.ngayTao = ngayTao;
        this.maGiaoDich = maGiaoDich;
        this.ghiChu = ghiChu;
    }

    public String getMaTT() { return maTT; }
    public void setMaTT(String maTT) { this.maTT = maTT; }

    public String getMaHD() { return maHD; }
    public void setMaHD(String maHD) { this.maHD = maHD; }

    public double getSoTien() { return soTien; }
    public void setSoTien(double soTien) { this.soTien = soTien; }

    public PhuongThucThanhToan getPhuongThuc() { return phuongThuc; }
    public void setPhuongThuc(PhuongThucThanhToan phuongThuc) { this.phuongThuc = phuongThuc; }

    public TrangThaiThanhToan getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiThanhToan trangThai) { this.trangThai = trangThai; }

    public String getNgayTao() { return ngayTao; }
    public void setNgayTao(String ngayTao) { this.ngayTao = ngayTao; }

    public String getMaGiaoDich() { return maGiaoDich; }
    public void setMaGiaoDich(String maGiaoDich) { this.maGiaoDich = maGiaoDich; }

    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
}
