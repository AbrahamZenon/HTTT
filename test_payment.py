# -*- coding: utf-8 -*-
"""
test_payment.py
Kiểm thử tự động cho UC02 - Thanh toán hóa đơn và quản lý trạng thái.
Kiểm tra các quy tắc nghiệp vụ BR04, BR05, BR10.
"""

import unittest
import os
from database import init_db, get_db_connection
from services import PaymentService, ThanhToanException
from models import TrangThaiHoaDon, TrangThaiThanhToan, PhuongThucThanhToan


class TestPaymentUC02(unittest.TestCase):

    def setUp(self):
        # Reset CSDL về trạng thái chuẩn trước mỗi test
        init_db(force_recreate=True)

    def test_01_get_invoices_and_detail(self):
        """Kiểm tra xem danh sách và chi tiết hóa đơn chờ thanh toán"""
        invoices = PaymentService.get_all_invoices(TrangThaiHoaDon.CHO_THANH_TOAN.value)
        self.assertGreaterEqual(len(invoices), 2)

        hd1 = PaymentService.get_invoice_detail("HD261115-001")
        self.assertIsNotNone(hd1)
        self.assertEqual(hd1["MaHD"], "HD261115-001")
        self.assertEqual(hd1["TrangThai"], TrangThaiHoaDon.CHO_THANH_TOAN.value)
        self.assertEqual(len(hd1["ChiTiet"]), 3)
        self.assertEqual(hd1["TongTien"], 475000.0)

    def test_02_payment_cash_success_and_inventory_decrement(self):
        """
        Kiểm tra thanh toán tiền mặt thành công:
        - BR04: Trạng thái hóa đơn chuyển thành DA_THANH_TOAN
        - BR05: Tạo bản ghi ThanhToan gắn liền với MaHD
        - BR10: Tồn kho của các lô thuốc tương ứng bị giảm chính xác
        """
        ma_hd = "HD261115-001"
        tong_tien = 475000.0
        tien_khach_tra = 500000.0

        # Kiểm tra tồn kho trước khi thanh toán
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute("SELECT SoLuongTon FROM LoThuoc WHERE MaLo = 'LO2026A' AND MaThuoc = 'TH01'")
        ton_truoc_lo_a = cursor.fetchone()["SoLuongTon"]
        conn.close()

        # Thực hiện thanh toán
        result = PaymentService.process_payment(
            ma_hd=ma_hd,
            phuong_thuc=PhuongThucThanhToan.TIEN_MAT.value,
            so_tien_khach_tra=tien_khach_tra,
            ma_nv="NV01",
            ghi_chu="Khách trả bằng tiền mặt"
        )

        self.assertTrue(result["success"])
        self.assertEqual(result["data"]["TienThua"], 25000.0)
        self.assertEqual(result["data"]["TrangThaiHoaDon"], TrangThaiHoaDon.DA_THANH_TOAN.value)

        # Kiểm tra CSDL sau thanh toán
        conn = get_db_connection()
        cursor = conn.cursor()

        # BR04: Kiểm tra trạng thái HĐ
        cursor.execute("SELECT TrangThai FROM HoaDon WHERE MaHD = ?", (ma_hd,))
        hd_status = cursor.fetchone()["TrangThai"]
        self.assertEqual(hd_status, TrangThaiHoaDon.DA_THANH_TOAN.value)

        # BR05: Kiểm tra bảng ThanhToan
        cursor.execute("SELECT * FROM ThanhToan WHERE MaHD = ?", (ma_hd,))
        tt = cursor.fetchone()
        self.assertIsNotNone(tt)
        self.assertEqual(tt["SoTien"], tong_tien)
        self.assertEqual(tt["TrangThai"], TrangThaiThanhToan.THANH_CONG.value)

        # BR10: Kiểm tra tồn kho đã giảm chính xác
        # Trong HD261115-001, TH01 (LO2026A) bán 1 đơn vị
        cursor.execute("SELECT SoLuongTon FROM LoThuoc WHERE MaLo = 'LO2026A' AND MaThuoc = 'TH01'")
        ton_sau_lo_a = cursor.fetchone()["SoLuongTon"]
        self.assertEqual(ton_sau_lo_a, ton_truoc_lo_a - 1)

        conn.close()

    def test_03_payment_cash_insufficient_amount(self):
        """Ngoại lệ: Tiền khách đưa nhỏ hơn tổng tiền hóa đơn"""
        ma_hd = "HD261115-002"  # Tổng tiền 164.000đ
        with self.assertRaises(ThanhToanException) as ctx:
            PaymentService.process_payment(
                ma_hd=ma_hd,
                phuong_thuc=PhuongThucThanhToan.TIEN_MAT.value,
                so_tien_khach_tra=100000.0  # Không đủ
            )
        self.assertIn("không đủ để thanh toán", str(ctx.exception))

    def test_04_payment_cannot_pay_already_paid_invoice(self):
        """Ngoại lệ: Không cho phép thanh toán hóa đơn đã thanh toán"""
        ma_hd = "HD261115-000"  # Đã thanh toán sẵn trong seed data
        with self.assertRaises(ThanhToanException) as ctx:
            PaymentService.process_payment(
                ma_hd=ma_hd,
                phuong_thuc=PhuongThucThanhToan.TIEN_MAT.value,
                so_tien_khach_tra=135000.0
            )
        self.assertIn("đã được thanh toán trước đó", str(ctx.exception))

    def test_05_cancel_invoice(self):
        """Quản lý trạng thái: Hủy hóa đơn đang chờ thanh toán"""
        ma_hd = "HD261115-002"
        res = PaymentService.cancel_invoice(ma_hd, "Khách đổi ý không mua")
        self.assertTrue(res["success"])
        self.assertEqual(res["data"]["TrangThai"], TrangThaiHoaDon.DA_HUY.value)

        # Hóa đơn đã hủy thì không thể thanh toán được nữa
        with self.assertRaises(ThanhToanException):
            PaymentService.process_payment(
                ma_hd=ma_hd,
                phuong_thuc=PhuongThucThanhToan.TIEN_MAT.value,
                so_tien_khach_tra=164000.0
            )

    def test_06_cannot_cancel_paid_invoice(self):
        """Không được phép hủy hóa đơn đã hoàn tất thanh toán"""
        ma_hd = "HD261115-000"
        with self.assertRaises(ThanhToanException) as ctx:
            PaymentService.cancel_invoice(ma_hd, "Thử hủy HĐ đã trả tiền")
        self.assertIn("Không thể hủy hóa đơn đã thanh toán", str(ctx.exception))


if __name__ == "__main__":
    unittest.main()

