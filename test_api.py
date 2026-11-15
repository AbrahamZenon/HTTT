# -*- coding: utf-8 -*-
"""
test_api.py
Kiểm tra tích hợp API HTTP của server.py
"""

import threading
import time
import urllib.request
import json
import socketserver
from server import LongChauAPIHandler, PORT
from database import init_db

def run_test():
    init_db(force_recreate=True)
    socketserver.TCPServer.allow_reuse_address = True
    server = socketserver.TCPServer(("", PORT), LongChauAPIHandler)
    server_thread = threading.Thread(target=server.serve_forever)
    server_thread.daemon = True
    server_thread.start()
    time.sleep(0.5)

    try:
        # Test 1: GET /api/stats
        req = urllib.request.urlopen(f"http://localhost:{PORT}/api/stats")
        data = json.loads(req.read().decode())
        assert data["success"] is True
        print("[OK] GET /api/stats thành công!")

        # Test 2: GET /api/invoices
        req = urllib.request.urlopen(f"http://localhost:{PORT}/api/invoices?status=CHO_THANH_TOAN")
        data = json.loads(req.read().decode())
        assert len(data["data"]) >= 2
        print(f"[OK] GET /api/invoices tìm thấy {len(data['data'])} hóa đơn chờ thanh toán!")

        # Test 3: POST /api/invoices/HD261115-001/pay
        pay_payload = json.dumps({
            "phuong_thuc": "TIEN_MAT",
            "so_tien_khach_tra": 500000,
            "ma_nv": "NV01"
        }).encode("utf-8")
        req = urllib.request.Request(
            f"http://localhost:{PORT}/api/invoices/HD261115-001/pay",
            data=pay_payload,
            headers={"Content-Type": "application/json"}
        )
        res = urllib.request.urlopen(req)
        pay_res = json.loads(res.read().decode())
        assert pay_res["success"] is True
        assert pay_res["data"]["TienThua"] == 25000.0
        print("[OK] POST /api/invoices/HD261115-001/pay thanh toán thành công!")

    finally:
        server.shutdown()
        server.server_close()
        print("Đã dừng server test.")

if __name__ == "__main__":
    run_test()

