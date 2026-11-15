# -*- coding: utf-8 -*-
"""
server.py
Web Server tích hợp sẵn (sử dụng thư viện chuẩn của Python, không cần cài đặt thêm gì).
Cung cấp REST API và Giao diện Web tương tác cho UC02 - Thanh toán hóa đơn (TV2).
Chạy lệnh: py server.py (hoặc python server.py)
Truy cập: http://localhost:8080
"""

import http.server
import socketserver
import json
import urllib.parse
import os
import sys

from database import init_db
from services import PaymentService, ThanhToanException

PORT = 8080
STATIC_DIR = os.path.join(os.path.dirname(__file__), "static")


class LongChauAPIHandler(http.server.SimpleHTTPRequestHandler):

    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=STATIC_DIR, **kwargs)

    def _send_json(self, status_code: int, data: dict):
        """Trả về phản hồi JSON cho Frontend"""
        response_bytes = json.dumps(data, ensure_ascii=False).encode("utf-8")
        self.send_response(status_code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(response_bytes)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.end_headers()
        self.wfile.write(response_bytes)

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.end_headers()

    def do_GET(self):
        parsed = urllib.parse.urlparse(self.path)
        path = parsed.path
        query_params = urllib.parse.parse_qs(parsed.query)

        # 1. API: Danh sách hóa đơn
        if path == "/api/invoices":
            status_filter = query_params.get("status", [None])[0]
            invoices = PaymentService.get_all_invoices(status_filter)
            self._send_json(200, {"success": True, "data": invoices})
            return

        # 2. API: Chi tiết hóa đơn
        elif path.startswith("/api/invoices/"):
            parts = path.strip("/").split("/")
            if len(parts) == 3:  # /api/invoices/<ma_hd>
                ma_hd = parts[2]
                inv = PaymentService.get_invoice_detail(ma_hd)
                if inv:
                    self._send_json(200, {"success": True, "data": inv})
                else:
                    self._send_json(404, {"success": False, "message": f"Không tìm thấy hóa đơn {ma_hd}"})
                return

        # 3. API: Báo cáo / Thống kê thanh toán
        elif path == "/api/stats":
            summary = PaymentService.get_payment_summary()
            self._send_json(200, {"success": True, "data": summary})
            return

        # 4. Phục vụ file tĩnh (HTML, CSS, JS)
        return super().do_GET()

    def do_POST(self):
        parsed = urllib.parse.urlparse(self.path)
        path = parsed.path

        content_length = int(self.headers.get("Content-Length", 0))
        post_data = self.rfile.read(content_length)
        payload = {}
        if post_data:
            try:
                payload = json.loads(post_data.decode("utf-8"))
            except Exception:
                payload = {}

        # 1. API: Thực hiện thanh toán hóa đơn
        if path.startswith("/api/invoices/") and path.endswith("/pay"):
            parts = path.strip("/").split("/")
            if len(parts) == 4:  # /api/invoices/<ma_hd>/pay
                ma_hd = parts[2]
                phuong_thuc = payload.get("phuong_thuc")
                so_tien_khach_tra = float(payload.get("so_tien_khach_tra", 0))
                ma_nv = payload.get("ma_nv", "NV01")
                ghi_chu = payload.get("ghi_chu", "")

                try:
                    result = PaymentService.process_payment(
                        ma_hd=ma_hd,
                        phuong_thuc=phuong_thuc,
                        so_tien_khach_tra=so_tien_khach_tra,
                        ma_nv=ma_nv,
                        ghi_chu=ghi_chu
                    )
                    self._send_json(200, result)
                except ThanhToanException as e:
                    self._send_json(400, {"success": False, "message": str(e)})
                except Exception as e:
                    self._send_json(500, {"success": False, "message": f"Lỗi hệ thống: {str(e)}"})
                return

        # 2. API: Hủy hóa đơn
        elif path.startswith("/api/invoices/") and path.endswith("/cancel"):
            parts = path.strip("/").split("/")
            if len(parts) == 4:  # /api/invoices/<ma_hd>/cancel
                ma_hd = parts[2]
                ly_do = payload.get("ly_do", "Khách hàng yêu cầu hủy")
                try:
                    result = PaymentService.cancel_invoice(ma_hd, ly_do)
                    self._send_json(200, result)
                except ThanhToanException as e:
                    self._send_json(400, {"success": False, "message": str(e)})
                except Exception as e:
                    self._send_json(500, {"success": False, "message": f"Lỗi hệ thống: {str(e)}"})
                return

        # 3. API: Khởi tạo lại CSDL mẫu để Demo
        elif path == "/api/reset-db":
            init_db(force_recreate=True)
            self._send_json(200, {"success": True, "message": "Đã reset CSDL và nạp dữ liệu mẫu Long Châu thành công!"})
            return

        self._send_json(404, {"success": False, "message": "API endpoint không tồn tại."})


def run_server():
    # Đảm bảo CSDL đã được khởi tạo
    init_db()

    # Cho phép tái sử dụng port tránh lỗi WinError 10048 khi restart nhanh
    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("", PORT), LongChauAPIHandler) as httpd:
        print("=" * 65)
        print(" HỆ THỐNG QUẢN LÝ BÁN HÀNG VÀ TỒN KHO NHÀ THUỐC LONG CHÂU")
        print(" Phân hệ TV2: UC02 - Thanh toán hóa đơn & Quản lý trạng thái")
        print("=" * 65)
        print(f" -> Web Server đang chạy tại: http://localhost:{PORT}")
        print(" -> Mở trình duyệt để trải nghiệm giao diện thanh toán!")
        print(" -> Nhấn Ctrl + C để dừng server.")
        print("=" * 65)
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nĐã dừng server.")


if __name__ == "__main__":
    run_server()

