package com.longchau.service;

import com.longchau.dao.Database;
import com.longchau.model.ChiTietPhieuNhap;
import com.longchau.model.LoThuoc;
import com.longchau.model.PhieuNhap;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NhapHangService {
    private final Database db = Database.getInstance();

    public PhieuNhap xacNhanNhap(String maPN, String maNCC, String maNV, String soChungTu,
                               List<ChiTietPhieuNhap> chiTiet) {
        String maPhieu = maPN == null ? "" : maPN.trim().toUpperCase(Locale.ROOT);
        if (maPhieu.isEmpty()) {
            throw new IllegalArgumentException("Nhập mã phiếu.");
        }
        if (chiTiet == null || chiTiet.isEmpty()) {
            throw new IllegalArgumentException("Thêm ít nhất một lô đạt kiểm tra.");
        }
        List<ChiTietPhieuNhap> cacDong = new ArrayList<>(chiTiet);
        synchronized (db) {
            if (maNCC == null || !db.nhaCungCaps.containsKey(maNCC)
                    || maNV == null || !db.nhanViens.containsKey(maNV)) {
                throw new IllegalArgumentException("Chọn nhà cung cấp và nhân viên hợp lệ.");
            }
            if (db.phieuNhaps.containsKey(maPhieu)) {
                throw new IllegalArgumentException("Mã phiếu đã tồn tại.");
            }

            LocalDateTime ngayNhap = LocalDateTime.now();
            Map<String, LoThuoc> tonMoi = new LinkedHashMap<>();
            for (ChiTietPhieuNhap dong : cacDong) {
                kiemTraDong(dong, ngayNhap.toLocalDate());
                String khoaLo = dong.getKhoaLo();
                if (tonMoi.containsKey(khoaLo)) {
                    throw new IllegalArgumentException("Lô thuốc trùng trên phiếu: " + dong.getMaLo());
                }
                LoThuoc lo = db.loThuocs.get(khoaLo);
                int ton = lo == null ? 0 : lo.getSoLuongTon();
                String hanDung = dong.getHanSuDung().toString();
                if (lo != null && !hanDung.equals(lo.getHanSuDung())) {
                    throw new IllegalArgumentException("Hạn sử dụng không khớp với lô " + dong.getMaLo());
                }
                if (ton > Integer.MAX_VALUE - dong.getSoLuongNhap()) {
                    throw new IllegalArgumentException("Tồn kho của lô " + dong.getMaLo() + " vượt giới hạn.");
                }
                tonMoi.put(khoaLo, new LoThuoc(dong.getMaLo(), dong.getMaThuoc(),
                                             hanDung, ton + dong.getSoLuongNhap()));
            }

            PhieuNhap phieu = new PhieuNhap(maPhieu, maNCC, maNV,
                    soChungTu == null ? "" : soChungTu.trim(), ngayNhap, cacDong);
            db.loThuocs.putAll(tonMoi);
            db.phieuNhaps.put(maPhieu, phieu);
            return phieu;
        }
    }

    public void kiemTraDong(ChiTietPhieuNhap dong, LocalDate homNay) {
        if (dong == null || dong.getMaThuoc() == null || !db.thuocs.containsKey(dong.getMaThuoc())) {
            throw new IllegalArgumentException("Chọn thuốc trong danh mục.");
        }
        if (dong.getMaLo().isEmpty() || dong.getMaLo().contains("#")) {
            throw new IllegalArgumentException("Nhập mã lô hợp lệ (không chứa dấu #).");
        }
        if (dong.getHanSuDung() == null || dong.getHanSuDung().isBefore(homNay)) {
            throw new IllegalArgumentException("Hạn sử dụng thiếu hoặc đã hết hạn ở lô " + dong.getMaLo());
        }
        if (dong.getSoLuongNhap() <= 0 || dong.getDonGiaNhap() <= 0) {
            throw new IllegalArgumentException("Số lượng đạt và đơn giá phải lớn hơn 0.");
        }
    }

    public List<PhieuNhap> getLichSu() {
        synchronized (db) {
            List<PhieuNhap> phieuNhaps = new ArrayList<>(db.phieuNhaps.values());
            phieuNhaps.sort(Comparator.comparing(PhieuNhap::getNgayNhap).reversed());
            return phieuNhaps;
        }
    }
}
