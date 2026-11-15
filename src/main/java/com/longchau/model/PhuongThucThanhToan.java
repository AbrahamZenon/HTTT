package com.longchau.model;

/**
 * Các phương thức thanh toán hỗ trợ tại quầy Long Châu
 */
public enum PhuongThucThanhToan {
    TIEN_MAT("Tiền mặt"),
    CHUYEN_KHOAN("Chuyển khoản QR"),
    THE("Thẻ ngân hàng / POS");

    private final String moTa;

    PhuongThucThanhToan(String moTa) {
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
