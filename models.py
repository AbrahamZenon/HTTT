# -*- coding: utf-8 -*-
"""
models.py
Mô hình thực thể (Entities) và Enums cho nghiệp vụ Quản lý hóa đơn và thanh toán (TV2).
Tuân thủ Baseline dự án Nhà thuốc Long Châu.
"""

from enum import Enum
from dataclasses import dataclass, field
from typing import List, Optional
from datetime import datetime


class TrangThaiHoaDon(str, Enum):
    CHO_THANH_TOAN = "CHO_THANH_TOAN"    # Mới lập từ TV1, chờ khách thanh toán
    DA_THANH_TOAN = "DA_THANH_TOAN"      # TV2 đã xử lý thanh toán thành công, hoàn tất HĐ
    DA_HUY = "DA_HUY"                    # Khách hủy hoặc quá hạn, không thu tiền


class TrangThaiThanhToan(str, Enum):
    DANG_XU_LY = "DANG_XU_LY"            # Chờ quẹt thẻ / quét mã QR
    THANH_CONG = "THANH_CONG"            # Tiền đã vào tài khoản / đã thu tiền mặt
    THAT_BAI = "THAT_BAI"                # Giao dịch lỗi / hủy giao dịch


class PhuongThucThanhToan(str, Enum):
    TIEN_MAT = "TIEN_MAT"                # Thanh toán tiền mặt tại quầy
    CHUYEN_KHOAN = "CHUYEN_KHOAN"        # Chuyển khoản VietQR
    THE = "THE"                          # Thẻ ATM / Visa / POS quẹt thẻ


@dataclass
class NhanVien:
    ma_nv: str
    ho_ten: str
    vai_tro: str                         # 'BAN_HANG', 'NHAP_HANG', 'QUAN_LY'
    ten_dang_nhap: str


@dataclass
class Thuoc:
    ma_thuoc: str
    ten_thuoc: str
    don_vi_tinh: str
    gia_ban: float
    trang_thai: str = "DANG_KINH_DOAN"   # 'DANG_KINH_DOAN', 'NGUNG_KINH_DOAN'


@dataclass
class LoThuoc:
    ma_lo: str
    ma_thuoc: str
    han_su_dung: str                     # Format: YYYY-MM-DD
    so_luong_ton: int


@dataclass
class ChiTietHoaDon:
    id: Optional[int]
    ma_hd: str
    ma_thuoc: str
    ma_lo: str
    so_luong: int
    don_gia: float
    # Thuộc tính mở rộng để hiển thị tiện ích
    ten_thuoc: Optional[str] = None
    don_vi_tinh: Optional[str] = None
    han_su_dung: Optional[str] = None

    @property
    def thanh_tien(self) -> float:
        return self.so_luong * self.don_gia


@dataclass
class ThanhToan:
    ma_tt: str
    ma_hd: str
    so_tien: float
    phuong_thuc: PhuongThucThanhToan
    trang_thai: TrangThaiThanhToan
    ngay_tao: str
    ma_giao_dich: Optional[str] = None
    ghi_chu: Optional[str] = None


@dataclass
class HoaDon:
    ma_hd: str
    ngay_lap: str
    tong_tien: float
    trang_thai: TrangThaiHoaDon
    ma_nv: str
    ghi_chu: Optional[str] = None
    chi_tiet: List[ChiTietHoaDon] = field(default_factory=list)
    thanh_toan: Optional[ThanhToan] = None

