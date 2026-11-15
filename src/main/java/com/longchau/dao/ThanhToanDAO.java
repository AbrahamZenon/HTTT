package com.longchau.dao;

import com.longchau.model.ThanhToan;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho Thanh toán (Giao dịch)
 */
public class ThanhToanDAO {
    private final Database db = Database.getInstance();

    public void insert(ThanhToan thanhToan) {
        db.thanhToans.put(thanhToan.getMaTT(), thanhToan);
        // Đồng thời gắn vào đối tượng HoaDon tương ứng
        if (db.hoaDons.containsKey(thanhToan.getMaHD())) {
            db.hoaDons.get(thanhToan.getMaHD()).setThanhToan(thanhToan);
        }
    }

    public ThanhToan findByMaHD(String maHD) {
        for (ThanhToan tt : db.thanhToans.values()) {
            if (tt.getMaHD().equals(maHD)) {
                return tt;
            }
        }
        return null;
    }

    public List<ThanhToan> findAll() {
        return new ArrayList<>(db.thanhToans.values());
    }
}

