# UC02 – Thanh toán hóa đơn nhà thuốc

Chạy trong thư mục dự án với Python 3.10 trở lên có Tkinter. Ứng dụng dùng SQLite tích hợp trong Python.

```powershell
python app.py --demo
```

1. Chọn **HD001** (57.000 ₫), chọn **Tiền mặt**, nhập `60000` và xác nhận. Hóa đơn chuyển sang **Đã thanh toán** và hiển thị tiền thừa 3.000 ₫.
2. Chọn **HD002** (90.000 ₫), chọn **Chuyển khoản**, nhập mã giao dịch mẫu `GD001` và xác nhận đã nhận đủ tiền. Mã giao dịch và thời gian thanh toán xuất hiện trên hóa đơn.
3. Mở lại ứng dụng để xem kết quả đã lưu. Dữ liệu mẫu được lưu riêng trong `thanh_toan_demo.sqlite3`.

Chạy `python app.py` để mở cơ sở dữ liệu riêng `thanh_toan.sqlite3`. UC02 nhận hóa đơn đã lập qua các bảng `HOADON`, `CHITIETHOADON` và `LOTHUOC`. Bản này chưa có màn hình lập hóa đơn UC01. Thanh toán chuyển khoản do nhân viên đối chiếu và xác nhận thủ công.

Mỗi hóa đơn được thanh toán toàn bộ một lần. Khi thành công, ứng dụng ghi `THANHTOAN`, đổi trạng thái hóa đơn và trừ tồn kho theo lô trong cùng một giao dịch SQLite. Nếu số tiền thiếu, mã giao dịch trùng, lô hết hạn hoặc tồn kho thiếu, dữ liệu giữ nguyên để nhân viên xử lý và thử lại.
