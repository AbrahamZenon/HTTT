package com.longchau.model;

/**
 * Trạng thái của Giao dịch thanh toán
 */
public enum TrangThaiThanhToan {
    DANG_XU_LY("Đang xử lý"),
    THANH_CONG("Thành công"),
    THAT_BAI("Thất bại");

    private final String moTa;

    TrangThaiThanhToan(String moTa) {
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
