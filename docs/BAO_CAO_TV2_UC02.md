# BÁO CÁO PHÂN TÍCH VÀ THIẾT KẾ PHÂN HỆ THÀNH VIÊN 2 (TV2)
## Đề tài: Hệ Thống Quản Lý Bán Hàng Và Tồn Kho Nhà Thuốc Long Châu
**Học phần:** Phân tích & Thiết kế Hệ thống Thông tin (Chuẩn mẫu báo cáo PTIT)  
**Thành viên phụ trách:** Thành viên 2 (TV2)  
**Use Case trọng tâm:** `UC02 – Thanh toán hóa đơn`  
**Nhiệm vụ bổ sung:** `Quản lý các trạng thái hóa đơn và thanh toán`  
**Ngôn ngữ triển khai:** `Java (JDK 8 – JDK 25+)` / `Java Swing UI`

---

## 1. Đặc tả Use Case chi tiết (UC02 - Thanh toán hóa đơn)

### 1.1. Bảng đặc tả Use Case
- **Mã Use Case:** UC02
- **Tên Use Case:** Thanh toán hóa đơn
- **Tác nhân chính:** Nhân viên bán hàng (Thu ngân)
- **Tác nhân phụ:** Khách hàng
- **Tiền điều kiện:** Hóa đơn đã được lập (bởi TV1) ở trạng thái `CHO_THANH_TOAN`. Khách hàng sẵn sàng thanh toán.
- **Hậu điều kiện:**
  - Hóa đơn chuyển sang trạng thái `DA_THANH_TOAN` (BR04).
  - Bản ghi `ThanhToan` được tạo liên kết với hóa đơn (BR05).
  - Tồn kho của các `LoThuoc` trong hóa đơn bị trừ chính xác (BR10).
  - Biên lai/hóa đơn bán lẻ được in ra cho khách.

---

## 2. Quản lý trạng thái (State Machine)
- `HoaDon`: `CHO_THANH_TOAN` -> `DA_THANH_TOAN` (khi thanh toán thành công) hoặc `DA_HUY` (khi khách hủy mua).
- `ThanhToan`: `DANG_XU_LY` -> `THANH_CONG` hoặc `THAT_BAI`.
- Không cho phép thanh toán lại hóa đơn đã hoàn tất hoặc đã hủy. Không cho phép hủy hóa đơn đã thanh toán.

---

## 3. Cấu trúc mã nguồn Java

```
c:\Users\kn200\HTTT\
├── run.bat                     # File kích hoạt 1-click chạy giao diện Swing
├── test.bat                    # File chạy 6 bài kiểm thử tự động BR04, BR05, BR10
├── src/main/java/com/longchau/
│   ├── model/                  # Tầng Entity & Enums
│   │   ├── TrangThaiHoaDon.java
│   │   ├── TrangThaiThanhToan.java
│   │   ├── PhuongThucThanhToan.java
│   │   ├── NhanVien.java
│   │   ├── Thuoc.java
│   │   ├── LoThuoc.java
│   │   ├── ChiTietHoaDon.java
│   │   ├── ThanhToan.java
│   │   └── HoaDon.java
│   ├── dao/                    # Tầng Truy xuất dữ liệu (Data Access Object)
│   │   ├── Database.java       # Mock DB quan hệ với Seed Data thực tế Long Châu
│   │   ├── HoaDonDAO.java      # Cập nhật & tra cứu Hóa đơn
│   │   ├── ThanhToanDAO.java   # Lưu giao dịch thanh toán
│   │   └── LoThuocDAO.java     # Kiểm tra & trừ tồn kho theo lô (BR10)
│   ├── service/                # Tầng Xử lý nghiệp vụ (Business Logic)
│   │   ├── PaymentException.java
│   │   ├── PaymentRequest.java
│   │   ├── PaymentResult.java
│   │   └── PaymentService.java # Kiểm tra BR04, BR05, BR10 & State Machine
│   ├── view/                   # Tầng Giao diện (Java Swing GUI)
│   │   ├── PaymentManagementFrame.java # Màn hình quầy thu ngân Long Châu
│   │   └── ReceiptDialog.java          # Modal in biên lai / hóa đơn bán lẻ
│   ├── Main.java               # Khởi chạy ứng dụng GUI
│   └── TestPaymentRunner.java  # Kiểm thử tự động 6 ca kiểm thử
```

---

## 4. Hướng dẫn chạy chương trình
1. Nhấp đúp chuột vào file `run.bat` để chạy giao diện Java Swing.
2. Nhấp đúp chuột vào file `test.bat` để chạy bộ kiểm thử tự động 6/6 test cases.
