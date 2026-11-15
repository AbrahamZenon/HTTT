package com.longchau.service;

import com.longchau.model.PhuongThucThanhToan;

/**
 * Data Transfer Object (DTO) chứa thông tin yêu cầu thanh toán gửi từ UI
 */
public class PaymentRequest {
    private String maHD;
    private PhuongThucThanhToan phuongThuc;
    private double soTienKhachTra;
    private String maNV;
    private String ghiChu;
    private String maGiaoDichRef; // Mã giao dịch ngoài nếu chuyển khoản hoặc quẹt thẻ

    public PaymentRequest() {}

    public PaymentRequest(String maHD, PhuongThucThanhToan phuongThuc, double soTienKhachTra, String maNV) {
        this.maHD = maHD;
        this.phuongThuc = phuongThuc;
        this.soTienKhachTra = soTienKhachTra;
        this.maNV = maNV;
    }

    public String getMaHD() { return maHD; }
    public void setMaHD(String maHD) { this.maHD = maHD; }

    public PhuongThucThanhToan getPhuongThuc() { return phuongThuc; }
    public void setPhuongThuc(PhuongThucThanhToan phuongThuc) { this.phuongThuc = phuongThuc; }

    public double getSoTienKhachTra() { return soTienKhachTra; }
    public void setSoTienKhachTra(double soTienKhachTra) { this.soTienKhachTra = soTienKhachTra; }

    public String getMaNV() { return maNV; }
    public void setMaNV(String maNV) { this.maNV = maNV; }

    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }

    public String getMaGiaoDichRef() { return maGiaoDichRef; }
    public void setMaGiaoDichRef(String maGiaoDichRef) { this.maGiaoDichRef = maGiaoDichRef; }
}
