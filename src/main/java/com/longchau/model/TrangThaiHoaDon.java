package com.longchau.model;

/**
 * Trạng thái của Hóa đơn bán hàng
 * Phụ trách bởi TV2: Quản lý các trạng thái hóa đơn
 */
public enum TrangThaiHoaDon {
    CHO_THANH_TOAN("Chờ thanh toán"),
    DA_THANH_TOAN("Đã thanh toán"),
    DA_HUY("Đã hủy");

    private final String moTa;

    TrangThaiHoaDon(String moTa) {
        this.moTa = moTa;
    }

    public String getMoTa() {
        return moTa;
    }

    @Override
    public String toString() {
        return moTa;
    }
}
