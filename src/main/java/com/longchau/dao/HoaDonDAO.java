package com.longchau.dao;

import com.longchau.model.HoaDon;
import com.longchau.model.TrangThaiHoaDon;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho Hóa đơn bán hàng
 */
public class HoaDonDAO {
    private final Database db = Database.getInstance();

    public List<HoaDon> findAll() {
        return new ArrayList<>(db.hoaDons.values());
    }

    public List<HoaDon> findByStatus(TrangThaiHoaDon status) {
        List<HoaDon> result = new ArrayList<>();
        for (HoaDon hd : db.hoaDons.values()) {
            if (hd.getTrangThai() == status) {
                result.add(hd);
            }
        }
        return result;
    }

    public HoaDon findById(String maHD) {
        return db.hoaDons.get(maHD);
    }

    public void updateStatus(String maHD, TrangThaiHoaDon newStatus) {
        HoaDon hd = db.hoaDons.get(maHD);
        if (hd != null) {
            hd.setTrangThai(newStatus);
        }
    }

    public void updateGhiChu(String maHD, String extraNote) {
        HoaDon hd = db.hoaDons.get(maHD);
        if (hd != null) {
            String current = hd.getGhiChu() != null ? hd.getGhiChu() : "";
            hd.setGhiChu(current + " " + extraNote);
        }
    }
}

