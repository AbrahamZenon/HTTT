package com.longchau.dao;

import com.longchau.model.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kho dữ liệu mô phỏng CSDL quan hệ Nhà thuốc Long Châu
 * Chứa Seed Data thực tế, hỗ trợ thao tác nguyên tử (Atomic / ACID simulation)
 */
public class Database {
    private static Database instance;

    public final Map<String, NhanVien> nhanViens = new ConcurrentHashMap<>();
    public final Map<String, Thuoc> thuocs = new ConcurrentHashMap<>();
    // Key của loThuocs là "MaLo#MaThuoc"
    public final Map<String, LoThuoc> loThuocs = new ConcurrentHashMap<>();
    public final Map<String, HoaDon> hoaDons = new ConcurrentHashMap<>();
    public final Map<String, ThanhToan> thanhToans = new ConcurrentHashMap<>();
    public final Map<String, NhaCungCap> nhaCungCaps = new ConcurrentHashMap<>();
    public final Map<String, PhieuNhap> phieuNhaps = new ConcurrentHashMap<>();

    private Database() {
        initSampleData();
    }

    public static synchronized Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    public synchronized void resetData() {
        nhanViens.clear();
        thuocs.clear();
        loThuocs.clear();
        hoaDons.clear();
        thanhToans.clear();
        nhaCungCaps.clear();
        phieuNhaps.clear();
        initSampleData();
    }

    private void initSampleData() {
        nhaCungCaps.put("NCC01", new NhaCungCap("NCC01", "Nhà cung cấp mẫu 1"));
        nhaCungCaps.put("NCC02", new NhaCungCap("NCC02", "Nhà cung cấp mẫu 2"));

        // 1. Nhân viên
        nhanViens.put("NV01", new NhanVien("NV01", "Nguyễn Văn An", "BAN_HANG", "an.nv"));
        nhanViens.put("NV02", new NhanVien("NV02", "Trần Thị Bình", "BAN_HANG", "binh.tt"));
        nhanViens.put("NV03", new NhanVien("NV03", "Lê Hoàng Long", "QUAN_LY", "long.lh"));

        // 2. Thuốc phổ biến tại Long Châu
        thuocs.put("TH01", new Thuoc("TH01", "Panadol Extra Đỏ (Hộp 15 vỉ x 12 viên)", "Hộp", 195000.0, "DANG_KINH_DOAN"));
        thuocs.put("TH02", new Thuoc("TH02", "Berocca Cationic Hưng Phấn (Tuýp 10 viên)", "Tuýp", 82000.0, "DANG_KINH_DOAN"));
        thuocs.put("TH03", new Thuoc("TH03", "Augmentin 1g Kháng sinh (Hộp 14 viên)", "Hộp", 245000.0, "DANG_KINH_DOAN"));
        thuocs.put("TH04", new Thuoc("TH04", "Paracetamol 500mg Domesco (Vỉ 10 viên)", "Vỉ", 12000.0, "DANG_KINH_DOAN"));
        thuocs.put("TH05", new Thuoc("TH05", "Nước muối sinh lý Natri Clorid 0.9% 500ml", "Chai", 10000.0, "DANG_KINH_DOAN"));
        thuocs.put("TH06", new Thuoc("TH06", "Siro ho Prospan Engelhard Đức (Chai 100ml)", "Chai", 135000.0, "DANG_KINH_DOAN"));

        // 3. Lô thuốc và tồn kho (Key: MaLo#MaThuoc)
        loThuocs.put("LO2026A#TH01", new LoThuoc("LO2026A", "TH01", "2027-12-31", 150));
        loThuocs.put("LO2026B#TH01", new LoThuoc("LO2026B", "TH01", "2026-12-15", 40));
        loThuocs.put("LO2025C#TH02", new LoThuoc("LO2025C", "TH02", "2027-08-20", 85));
        loThuocs.put("LO2026D#TH03", new LoThuoc("LO2026D", "TH03", "2027-05-10", 60));
        loThuocs.put("LO2026E#TH04", new LoThuoc("LO2026E", "TH04", "2028-01-01", 300));
        loThuocs.put("LO2025F#TH05", new LoThuoc("LO2025F", "TH05", "2028-06-30", 200));
        loThuocs.put("LO2025G#TH06", new LoThuoc("LO2025G", "TH06", "2026-11-30", 25));

        // 4. Hóa đơn mẫu:
        // Hóa đơn 1: Đang chờ thanh toán (CHO_THANH_TOAN) do TV1 lập - để TV2 thanh toán
        HoaDon hd1 = new HoaDon("HD261115-001", "2026-11-15 19:30:00", 475000.0,
                TrangThaiHoaDon.CHO_THANH_TOAN, "NV01", "Khách mua tại quầy 1");
        hd1.getChiTiet().add(createItem(1, "HD261115-001", "TH01", "LO2026A", 1, 195000.0));
        hd1.getChiTiet().add(createItem(2, "HD261115-001", "TH03", "LO2026D", 1, 245000.0));
        hd1.getChiTiet().add(createItem(3, "HD261115-001", "TH05", "LO2025F", 3, 10000.0));
        hoaDons.put(hd1.getMaHD(), hd1);

        // Hóa đơn 2: Chờ thanh toán nhỏ
        HoaDon hd2 = new HoaDon("HD261115-002", "2026-11-15 20:15:00", 164000.0,
                TrangThaiHoaDon.CHO_THANH_TOAN, "NV02", "Khách mua cảm cúm");
        hd2.getChiTiet().add(createItem(1, "HD261115-002", "TH02", "LO2025C", 2, 82000.0));
        hoaDons.put(hd2.getMaHD(), hd2);

        // Hóa đơn 3: Đã thanh toán hoàn tất trước đó
        HoaDon hd0 = new HoaDon("HD261115-000", "2026-11-15 18:00:00", 135000.0,
                TrangThaiHoaDon.DA_THANH_TOAN, "NV01", "Khách quen");
        hd0.getChiTiet().add(createItem(1, "HD261115-000", "TH06", "LO2025G", 1, 135000.0));

        ThanhToan tt0 = new ThanhToan("TT261115-000", "HD261115-000", 135000.0,
                PhuongThucThanhToan.TIEN_MAT, TrangThaiThanhToan.THANH_CONG,
                "2026-11-15 18:02:15", "TM-9921", "Khách đưa đủ");
        hd0.setThanhToan(tt0);
        hoaDons.put(hd0.getMaHD(), hd0);
        thanhToans.put(tt0.getMaTT(), tt0);
    }

    private ChiTietHoaDon createItem(int id, String maHD, String maThuoc, String maLo, int sl, double donGia) {
        ChiTietHoaDon item = new ChiTietHoaDon(id, maHD, maThuoc, maLo, sl, donGia);
        Thuoc t = thuocs.get(maThuoc);
        if (t != null) {
            item.setTenThuoc(t.getTenThuoc());
            item.setDonViTinh(t.getDonViTinh());
        }
        LoThuoc lt = loThuocs.get(maLo + "#" + maThuoc);
        if (lt != null) {
            item.setHanSuDung(lt.getHanSuDung());
        }
        return item;
    }
}

