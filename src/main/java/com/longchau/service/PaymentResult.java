package com.longchau.service;

import com.longchau.model.ThanhToan;
import com.longchau.model.TrangThaiHoaDon;

/**
 * Data Transfer Object (DTO) chứa kết quả thanh toán
 */
public class PaymentResult {
    private boolean success;
    private String message;
    private ThanhToan thanhToan;
    private double tienThua;
    private TrangThaiHoaDon trangThaiHoaDon;

    public PaymentResult() {}

    public PaymentResult(boolean success, String message, ThanhToan thanhToan, double tienThua, TrangThaiHoaDon trangThaiHoaDon) {
        this.success = success;
        this.message = message;
        this.thanhToan = thanhToan;
        this.tienThua = tienThua;
        this.trangThaiHoaDon = trangThaiHoaDon;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public ThanhToan getThanhToan() { return thanhToan; }
    public void setThanhToan(ThanhToan thanhToan) { this.thanhToan = thanhToan; }

    public double getTienThua() { return tienThua; }
    public void setTienThua(double tienThua) { this.tienThua = tienThua; }

    public TrangThaiHoaDon getTrangThaiHoaDon() { return trangThaiHoaDon; }
    public void setTrangThaiHoaDon(TrangThaiHoaDon trangThaiHoaDon) { this.trangThaiHoaDon = trangThaiHoaDon; }
}
