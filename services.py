# -*- coding: utf-8 -*-
"""
services.py
Lớp nghiệp vụ (Business Service Layer) cho UC02 - Thanh toán hóa đơn & Quản lý trạng thái.
Hiện thực hóa các quy tắc:
  - BR04: Hóa đơn chỉ được hoàn tất khi thanh toán thành công.
  - BR05: Thanh toán phải gắn với một hóa đơn cụ thể.
  - BR10: Số lượng tồn kho giảm khi hóa đơn được thanh toán và hoàn tất.
  - Quản lý trạng thái Hóa đơn và Thanh toán chặt chẽ (State Machine).
"""

from typing import List, Dict, Any, Optional, Tuple
from datetime import datetime
import uuid

from database import get_db_connection
from models import (
    HoaDon, ChiTietHoaDon, ThanhToan,
    TrangThaiHoaDon, TrangThaiThanhToan, PhuongThucThanhToan
)


class ThanhToanException(Exception):
    """Lớp ngoại lệ cho các lỗi nghiệp vụ thanh toán"""
    pass


class PaymentService:
    """Service xử lý toàn bộ logic nghiệp vụ thanh toán (Nhiệm vụ TV2)"""

    @staticmethod
    def get_all_invoices(status_filter: Optional[str] = None) -> List[Dict[str, Any]]:
        """Lấy danh sách hóa đơn theo trạng thái (hoặc tất cả)"""
        conn = get_db_connection()
        cursor = conn.cursor()

        query = """
        SELECT hd.MaHD, hd.NgayLap, hd.TongTien, hd.TrangThai, hd.MaNV, hd.GhiChu,
               nv.HoTen AS TenNV,
               tt.MaTT, tt.PhuongThuc, tt.TrangThai AS TrangThaiTT, tt.NgayTao AS NgayThanhToan
        FROM HoaDon hd
        JOIN NhanVien nv ON hd.MaNV = nv.MaNV
        LEFT JOIN ThanhToan tt ON hd.MaHD = tt.MaHD
        """
        params = []
        if status_filter:
            query += " WHERE hd.TrangThai = ?"
            params.append(status_filter)

        query += " ORDER BY hd.NgayLap DESC;"
        cursor.execute(query, params)
        rows = cursor.fetchall()
        conn.close()

        return [dict(row) for row in rows]

    @staticmethod
    def get_invoice_detail(ma_hd: str) -> Optional[Dict[str, Any]]:
        """Lấy chi tiết một hóa đơn kèm danh sách thuốc và thông tin thanh toán (nếu có)"""
        conn = get_db_connection()
        cursor = conn.cursor()

        # 1. Thông tin hóa đơn
        cursor.execute("""
        SELECT hd.MaHD, hd.NgayLap, hd.TongTien, hd.TrangThai, hd.MaNV, hd.GhiChu,
               nv.HoTen AS TenNV
        FROM HoaDon hd
        JOIN NhanVien nv ON hd.MaNV = nv.MaNV
        WHERE hd.MaHD = ?;
        """, (ma_hd,))
        hd_row = cursor.fetchone()
        if not hd_row:
            conn.close()
            return None

        invoice_data = dict(hd_row)

        # 2. Chi tiết danh sách thuốc & lô thuốc bán
        cursor.execute("""
        SELECT ct.Id, ct.MaHD, ct.MaThuoc, ct.MaLo, ct.SoLuong, ct.DonGia,
               (ct.SoLuong * ct.DonGia) AS ThanhTien,
               t.TenThuoc, t.DonViTinh,
               lt.HanSuDung, lt.SoLuongTon
        FROM ChiTietHoaDon ct
        JOIN Thuoc t ON ct.MaThuoc = t.MaThuoc
        JOIN LoThuoc lt ON (ct.MaLo = lt.MaLo AND ct.MaThuoc = lt.MaThuoc)
        WHERE ct.MaHD = ?;
        """, (ma_hd,))
        items = [dict(row) for row in cursor.fetchall()]
        invoice_data["ChiTiet"] = items

        # 3. Thông tin thanh toán (nếu đã có)
        cursor.execute("""
        SELECT MaTT, MaHD, SoTien, PhuongThuc, TrangThai, NgayTao, MaGiaoDich, GhiChu
        FROM ThanhToan
        WHERE MaHD = ?;
        """, (ma_hd,))
        tt_row = cursor.fetchone()
        invoice_data["ThanhToan"] = dict(tt_row) if tt_row else None

        conn.close()
        return invoice_data

    @staticmethod
    def process_payment(
        ma_hd: str,
        phuong_thuc: str,
        so_tien_khach_tra: float,
        ma_nv: str = "NV01",
        ghi_chu: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Hiện thực hóa UC02 - Thanh toán hóa đơn:
        1. Kiểm tra sự tồn tại và tính hợp lệ của hóa đơn.
        2. Kiểm tra trạng thái hóa đơn (Chỉ cho phép thanh toán hóa đơn CHO_THANH_TOAN).
        3. Kiểm tra số tiền thanh toán (với tiền mặt phải >= tổng tiền).
        4. Tạo giao dịch ThanhToan (BR05).
        5. Cập nhật trạng thái Hóa đơn sang DA_THANH_TOAN (BR04).
        6. Giảm số lượng tồn kho theo từng lô thuốc (BR10).
        7. Đảm bảo toàn vẹn dữ liệu qua SQLite Transaction (ACID).
        """
        conn = get_db_connection()
        cursor = conn.cursor()

        try:
            # Bước 1: Khóa và lấy thông tin Hóa đơn
            cursor.execute("SELECT MaHD, TongTien, TrangThai FROM HoaDon WHERE MaHD = ?", (ma_hd,))
            hd = cursor.fetchone()
            if not hd:
                raise ThanhToanException(f"Không tìm thấy hóa đơn mã '{ma_hd}'.")

            # Bước 2: Kiểm tra trạng thái hiện tại (State Machine check)
            current_status = hd["TrangThai"]
            if current_status == TrangThaiHoaDon.DA_THANH_TOAN.value:
                raise ThanhToanException(f"Hóa đơn '{ma_hd}' đã được thanh toán trước đó. Không thể thanh toán lại!")
            if current_status == TrangThaiHoaDon.DA_HUY.value:
                raise ThanhToanException(f"Hóa đơn '{ma_hd}' đã bị hủy. Không thể thanh toán!")
            if current_status != TrangThaiHoaDon.CHO_THANH_TOAN.value:
                raise ThanhToanException(f"Trạng thái hóa đơn '{current_status}' không hợp lệ để thanh toán.")

            tong_tien = float(hd["TongTien"])

            # Bước 3: Kiểm tra phương thức và số tiền
            if phuong_thuc not in [p.value for p in PhuongThucThanhToan]:
                raise ThanhToanException(f"Phương thức thanh toán '{phuong_thuc}' không hợp lệ.")

            tien_thua = 0.0
            ma_giao_dich = None

            if phuong_thuc == PhuongThucThanhToan.TIEN_MAT.value:
                if so_tien_khach_tra < tong_tien:
                    raise ThanhToanException(
                        f"Số tiền khách đưa ({so_tien_khach_tra:,.0f}đ) không đủ để thanh toán ({tong_tien:,.0f}đ)!"
                    )
                tien_thua = so_tien_khach_tra - tong_tien
                ma_giao_dich = f"CASH-{datetime.now().strftime('%y%m%d%H%M%S')}"

            elif phuong_thuc == PhuongThucThanhToan.CHUYEN_KHOAN.value:
                # Chuyển khoản QR code
                ma_giao_dich = f"QR-{uuid.uuid4().hex[:8].upper()}"

            elif phuong_thuc == PhuongThucThanhToan.THE.value:
                # Quẹt thẻ POS
                ma_giao_dich = f"POS-{uuid.uuid4().hex[:8].upper()}"

            # Bước 4: Kiểm tra tồn kho của các lô thuốc TRƯỚC KHI TRỪ (BR10)
            cursor.execute("""
            SELECT ct.MaThuoc, ct.MaLo, ct.SoLuong, lt.SoLuongTon, t.TenThuoc
            FROM ChiTietHoaDon ct
            JOIN LoThuoc lt ON (ct.MaLo = lt.MaLo AND ct.MaThuoc = lt.MaThuoc)
            JOIN Thuoc t ON ct.MaThuoc = t.MaThuoc
            WHERE ct.MaHD = ?;
            """, (ma_hd,))
            chi_tiets = cursor.fetchall()

            for item in chi_tiets:
                ma_thuoc = item["MaThuoc"]
                ma_lo = item["MaLo"]
                so_luong_can_tru = item["SoLuong"]
                so_luong_ton = item["SoLuongTon"]
                ten_thuoc = item["TenThuoc"]

                if so_luong_ton < so_luong_can_tru:
                    raise ThanhToanException(
                        f"Lỗi tồn kho: Thuốc '{ten_thuoc}' (Lô: {ma_lo}) chỉ còn {so_luong_ton} "
                        f"không đủ để bán {so_luong_can_tru}!"
                    )

            # Bước 5: Bắt đầu Transaction ghi nhận thanh toán và trừ kho
            # 5.1 Tạo bản ghi ThanhToan (BR05)
            ma_tt = f"TT{datetime.now().strftime('%y%m%d')}-{uuid.uuid4().hex[:6].upper()}"
            ngay_tao = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

            cursor.execute("""
            INSERT INTO ThanhToan (MaTT, MaHD, SoTien, PhuongThuc, TrangThai, NgayTao, MaGiaoDich, GhiChu)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?);
            """, (
                ma_tt,
                ma_hd,
                tong_tien,
                phuong_thuc,
                TrangThaiThanhToan.THANH_CONG.value,
                ngay_tao,
                ma_giao_dich,
                ghi_chu or f"Thanh toán hoàn tất bởi nhân viên {ma_nv}"
            ))

            # 5.2 Cập nhật trạng thái Hóa đơn sang DA_THANH_TOAN (BR04)
            cursor.execute("""
            UPDATE HoaDon
            SET TrangThai = ?
            WHERE MaHD = ?;
            """, (TrangThaiHoaDon.DA_THANH_TOAN.value, ma_hd))

            # 5.3 Trừ tồn kho tại bảng LoThuoc (BR10)
            for item in chi_tiets:
                cursor.execute("""
                UPDATE LoThuoc
                SET SoLuongTon = SoLuongTon - ?
                WHERE MaLo = ? AND MaThuoc = ?;
                """, (item["SoLuong"], item["MaLo"], item["MaThuoc"]))

            # Hoàn tất Transaction
            conn.commit()

            return {
                "success": True,
                "message": "Thanh toán thành công và đã cập nhật tồn kho!",
                "data": {
                    "MaTT": ma_tt,
                    "MaHD": ma_hd,
                    "TongTien": tong_tien,
                    "PhuongThuc": phuong_thuc,
                    "MaGiaoDich": ma_giao_dich,
                    "SoTienKhachTra": so_tien_khach_tra,
                    "TienThua": tien_thua,
                    "NgayThanhToan": ngay_tao,
                    "TrangThaiHoaDon": TrangThaiHoaDon.DA_THANH_TOAN.value
                }
            }

        except Exception as e:
            conn.rollback()
            raise e
        finally:
            conn.close()

    @staticmethod
    def cancel_invoice(ma_hd: str, reason: str = "Khách hủy mua hàng") -> Dict[str, Any]:
        """
        Nghiệp vụ Hủy hóa đơn (Quản lý trạng thái):
        - Chỉ cho phép hủy hóa đơn ở trạng thái CHO_THANH_TOAN.
        - Không được hủy hóa đơn đã thanh toán (trừ khi có nghiệp vụ hoàn trả phức tạp nằm ngoài phạm vi).
        """
        conn = get_db_connection()
        cursor = conn.cursor()

        try:
            cursor.execute("SELECT MaHD, TrangThai FROM HoaDon WHERE MaHD = ?", (ma_hd,))
            hd = cursor.fetchone()
            if not hd:
                raise ThanhToanException(f"Không tìm thấy hóa đơn mã '{ma_hd}'.")

            current_status = hd["TrangThai"]
            if current_status == TrangThaiHoaDon.DA_THANH_TOAN.value:
                raise ThanhToanException("Không thể hủy hóa đơn đã thanh toán hoàn tất!")
            if current_status == TrangThaiHoaDon.DA_HUY.value:
                raise ThanhToanException("Hóa đơn này đã được hủy trước đó.")

            cursor.execute("""
            UPDATE HoaDon
            SET TrangThai = ?, GhiChu = GhiChu || ?
            WHERE MaHD = ?;
            """, (TrangThaiHoaDon.DA_HUY.value, f" [Đã hủy: {reason}]", ma_hd))

            conn.commit()
            return {
                "success": True,
                "message": f"Đã hủy hóa đơn '{ma_hd}' thành công.",
                "data": {"MaHD": ma_hd, "TrangThai": TrangThaiHoaDon.DA_HUY.value}
            }
        except Exception as e:
            conn.rollback()
            raise e
        finally:
            conn.close()

    @staticmethod
    def get_payment_summary() -> Dict[str, Any]:
        """Thống kê trạng thái hóa đơn và doanh thu phục vụ báo cáo nhanh của TV2"""
        conn = get_db_connection()
        cursor = conn.cursor()

        # Số lượng theo trạng thái HĐ
        cursor.execute("""
        SELECT TrangThai, COUNT(*) as SoLuong, COALESCE(SUM(TongTien), 0) as TongGiaTri
        FROM HoaDon
        GROUP BY TrangThai;
        """)
        rows = cursor.fetchall()
        status_counts = {row["TrangThai"]: {"SoLuong": row["SoLuong"], "TongGiaTri": row["TongGiaTri"]} for row in rows}

        # Thống kê theo phương thức thanh toán
        cursor.execute("""
        SELECT PhuongThuc, COUNT(*) as SoGiaoDich, COALESCE(SUM(SoTien), 0) as DoanhThu
        FROM ThanhToan
        WHERE TrangThai = 'THANH_CONG'
        GROUP BY PhuongThuc;
        """)
        pt_rows = cursor.fetchall()
        payment_methods = {row["PhuongThuc"]: {"SoGiaoDich": row["SoGiaoDich"], "DoanhThu": row["DoanhThu"]} for row in pt_rows}

        conn.close()
        return {
            "trang_thai_hoa_don": status_counts,
            "phuong_thuc_thanh_toan": payment_methods
        }

