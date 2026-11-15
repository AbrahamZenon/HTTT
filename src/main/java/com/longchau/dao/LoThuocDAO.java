package com.longchau.dao;

import com.longchau.model.LoThuoc;

/**
 * Data Access Object cho Lô thuốc và Tồn kho
 * Phục vụ quy tắc BR10: Giảm tồn kho theo lô khi thanh toán hoàn tất
 */
public class LoThuocDAO {
    private final Database db = Database.getInstance();

    public LoThuoc find(String maLo, String maThuoc) {
        String key = maLo + "#" + maThuoc;
        return db.loThuocs.get(key);
    }

    public synchronized boolean hasEnoughStock(String maLo, String maThuoc, int requiredQty) {
        LoThuoc lt = find(maLo, maThuoc);
        return lt != null && lt.getSoLuongTon() >= requiredQty;
    }

    public synchronized void decreaseStock(String maLo, String maThuoc, int quantity) {
        LoThuoc lt = find(maLo, maThuoc);
        if (lt != null) {
            int newQty = lt.getSoLuongTon() - quantity;
            if (newQty < 0) {
                throw new IllegalStateException("Số lượng tồn kho không được âm! Lô: " + maLo);
            }
            lt.setSoLuongTon(newQty);
        }
    }
}

