# -*- coding: utf-8 -*-
"""
database.py
Khởi tạo cơ sở dữ liệu SQLite và nạp dữ liệu mẫu (Seed Data)
Đảm bảo liên kết thực thể theo đúng Baseline dự án Nhà thuốc Long Châu.
"""

import sqlite3
import os
from datetime import datetime, date

DB_PATH = os.path.join(os.path.dirname(__file__), "longchau_pharmacy.db")


def get_db_connection():
    """Tạo kết nối tới CSDL SQLite, bật Foreign Key constraint"""
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON;")
    return conn


def init_db(force_recreate=False):
    """Khởi tạo cấu trúc bảng CSDL chuẩn quan hệ"""
    if force_recreate and os.path.exists(DB_PATH):
        os.remove(DB_PATH)

    conn = get_db_connection()
    cursor = conn.cursor()

    # 1. Bảng Nhân viên
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS NhanVien (
        MaNV TEXT PRIMARY KEY,
        HoTen TEXT NOT NULL,
        VaiTro TEXT NOT NULL,
        TenDangNhap TEXT UNIQUE NOT NULL
    );
    """)

    # 2. Bảng Thuốc
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS Thuoc (
        MaThuoc TEXT PRIMARY KEY,
        TenThuoc TEXT NOT NULL,
        DonViTinh TEXT NOT NULL,
        GiaBan REAL NOT NULL CHECK(GiaBan >= 0),
        TrangThai TEXT NOT NULL DEFAULT 'DANG_KINH_DOAN'
    );
    """)

    # 3. Bảng Lô thuốc (Mỗi thuốc có thể có nhiều lô, lưu hạn dùng và tồn kho)
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS LoThuoc (
        MaLo TEXT NOT NULL,
        MaThuoc TEXT NOT NULL,
        HanSuDung TEXT NOT NULL,
        SoLuongTon INTEGER NOT NULL CHECK(SoLuongTon >= 0),
        PRIMARY KEY (MaLo, MaThuoc),
        FOREIGN KEY (MaThuoc) REFERENCES Thuoc(MaThuoc)
    );
    """)

    # 4. Bảng Nhà cung cấp (Hỗ trợ nghiệp vụ nhập hàng của TV3)
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS NhaCungCap (
        MaNCC TEXT PRIMARY KEY,
        TenNCC TEXT NOT NULL,
        DiaChi TEXT,
        SoDienThoai TEXT
    );
    """)

    # 5. Bảng Hóa đơn bán hàng
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS HoaDon (
        MaHD TEXT PRIMARY KEY,
        NgayLap TEXT NOT NULL,
        TongTien REAL NOT NULL CHECK(TongTien >= 0),
        TrangThai TEXT NOT NULL DEFAULT 'CHO_THANH_TOAN',
        MaNV TEXT NOT NULL,
        GhiChu TEXT,
        FOREIGN KEY (MaNV) REFERENCES NhanVien(MaNV)
    );
    """)

    # 6. Bảng Chi tiết hóa đơn (Liên kết Thuốc và Lô thuốc để truy vết và trừ kho BR10)
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS ChiTietHoaDon (
        Id INTEGER PRIMARY KEY AUTOINCREMENT,
        MaHD TEXT NOT NULL,
        MaThuoc TEXT NOT NULL,
        MaLo TEXT NOT NULL,
        SoLuong INTEGER NOT NULL CHECK(SoLuong > 0),
        DonGia REAL NOT NULL CHECK(DonGia >= 0),
        FOREIGN KEY (MaHD) REFERENCES HoaDon(MaHD) ON DELETE CASCADE,
        FOREIGN KEY (MaLo, MaThuoc) REFERENCES LoThuoc(MaLo, MaThuoc)
    );
    """)

    # 7. Bảng Thanh toán (Gắn với Hóa đơn BR05)
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS ThanhToan (
        MaTT TEXT PRIMARY KEY,
        MaHD TEXT UNIQUE NOT NULL,
        SoTien REAL NOT NULL CHECK(SoTien >= 0),
        PhuongThuc TEXT NOT NULL,
        TrangThai TEXT NOT NULL,
        NgayTao TEXT NOT NULL,
        MaGiaoDich TEXT,
        GhiChu TEXT,
        FOREIGN KEY (MaHD) REFERENCES HoaDon(MaHD)
    );
    """)

    conn.commit()

    # Nạp dữ liệu mẫu nếu chưa có
    cursor.execute("SELECT COUNT(*) as count FROM Thuoc")
    if cursor.fetchone()["count"] == 0:
        seed_sample_data(conn)

    conn.close()


def seed_sample_data(conn):
    """Nạp dữ liệu thực tế mẫu để phục vụ kiểm thử và thuyết trình"""
    cursor = conn.cursor()

    # 1. Nhân viên
    nhan_viens = [
        ("NV01", "Nguyễn Văn An", "BAN_HANG", "an.nv"),
        ("NV02", "Trần Thị Bình", "BAN_HANG", "binh.tt"),
        ("NV03", "Lê Hoàng Long", "QUAN_LY", "long.lh"),
    ]
    cursor.executemany("INSERT INTO NhanVien VALUES (?, ?, ?, ?)", nhan_viens)

    # 2. Danh mục thuốc Long Châu phổ biến
    thuocs = [
        ("TH01", "Panadol Extra Đỏ (Hộp 15 vỉ x 12 viên)", "Hộp", 195000.0, "DANG_KINH_DOAN"),
        ("TH02", "Berocca Cationic Hưng Phấn (Tuýp 10 viên)", "Tuýp", 82000.0, "DANG_KINH_DOAN"),
        ("TH03", "Augmentin 1g Kháng sinh (Hộp 14 viên)", "Hộp", 245000.0, "DANG_KINH_DOAN"),
        ("TH04", "Paracetamol 500mg Domesco (Vỉ 10 viên)", "Vỉ", 12000.0, "DANG_KINH_DOAN"),
        ("TH05", "Nước muối sinh lý Natri Clorid 0.9% 500ml", "Chai", 10000.0, "DANG_KINH_DOAN"),
        ("TH06", "Siro ho Prospan Engelhard Đức (Chai 100ml)", "Chai", 135000.0, "DANG_KINH_DOAN"),
    ]
    cursor.executemany("INSERT INTO Thuoc VALUES (?, ?, ?, ?, ?)", thuocs)

    # 3. Lô thuốc và hạn sử dụng (Có lô hạn xa và lô sắp hết hạn để test)
    lo_thuocs = [
        ("LO2026A", "TH01", "2027-12-31", 150),
        ("LO2026B", "TH01", "2026-12-15", 40),
        ("LO2025C", "TH02", "2027-08-20", 85),
        ("LO2026D", "TH03", "2027-05-10", 60),
        ("LO2026E", "TH04", "2028-01-01", 300),
        ("LO2025F", "TH05", "2028-06-30", 200),
        ("LO2025G", "TH06", "2026-11-30", 25),  # Lô sắp hết hạn
    ]
    cursor.executemany("INSERT INTO LoThuoc VALUES (?, ?, ?, ?)", lo_thuocs)

    # 4. Hóa đơn mẫu:
    # Hóa đơn 1: Đang chờ thanh toán (CHO_THANH_TOAN) - để TV2 thực hiện thanh toán ngay
    cursor.execute("""
    INSERT INTO HoaDon (MaHD, NgayLap, TongTien, TrangThai, MaNV, GhiChu)
    VALUES ('HD261115-001', '2026-11-15 19:30:00', 475000.0, 'CHO_THANH_TOAN', 'NV01', 'Khách mua tại quầy 1')
    """)
    cursor.execute("""
    INSERT INTO ChiTietHoaDon (MaHD, MaThuoc, MaLo, SoLuong, DonGia)
    VALUES 
        ('HD261115-001', 'TH01', 'LO2026A', 1, 195000.0),
        ('HD261115-001', 'TH03', 'LO2026D', 1, 245000.0),
        ('HD261115-001', 'TH05', 'LO2025F', 3, 10000.0)
    """)

    # Hóa đơn 2: Chờ thanh toán nhỏ
    cursor.execute("""
    INSERT INTO HoaDon (MaHD, NgayLap, TongTien, TrangThai, MaNV, GhiChu)
    VALUES ('HD261115-002', '2026-11-15 20:15:00', 164000.0, 'CHO_THANH_TOAN', 'NV02', 'Khách mua cảm sốt')
    """)
    cursor.execute("""
    INSERT INTO ChiTietHoaDon (MaHD, MaThuoc, MaLo, SoLuong, DonGia)
    VALUES 
        ('HD261115-002', 'TH02', 'LO2025C', 2, 82000.0)
    """)

    # Hóa đơn 3: Đã thanh toán trước đó (DA_THANH_TOAN)
    cursor.execute("""
    INSERT INTO HoaDon (MaHD, NgayLap, TongTien, TrangThai, MaNV, GhiChu)
    VALUES ('HD261115-000', '2026-11-15 18:00:00', 135000.0, 'DA_THANH_TOAN', 'NV01', 'Khách quen')
    """)
    cursor.execute("""
    INSERT INTO ChiTietHoaDon (MaHD, MaThuoc, MaLo, SoLuong, DonGia)
    VALUES ('HD261115-000', 'TH06', 'LO2025G', 1, 135000.0)
    """)
    cursor.execute("""
    INSERT INTO ThanhToan (MaTT, MaHD, SoTien, PhuongThuc, TrangThai, NgayTao, MaGiaoDich, GhiChu)
    VALUES ('TT261115-000', 'HD261115-000', 135000.0, 'TIEN_MAT', 'THANH_CONG', '2026-11-15 18:02:15', 'TM-9921', 'Khách đưa đủ')
    """)

    conn.commit()


if __name__ == "__main__":
    init_db(force_recreate=True)
    print("Khởi tạo CSDL SQLite longchau_pharmacy.db và Seed Data thành công!")

