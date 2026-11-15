# Hệ Thống Quản Lý Bán Hàng & Tồn Kho Nhà Thuốc Long Châu
## Phân Hệ Thành Viên 2 (TV2): UC02 - Thanh Toán Hóa Đơn & Quản Lý Trạng Thái

Dự án được xây dựng theo chuẩn mô hình phân tích thiết kế hướng đối tượng (OOAD/PTIT) bằng ngôn ngữ **Java**, độc lập không phụ thuộc thư viện ngoài (Zero Dependency), sẵn sàng biên dịch và chạy trên mọi môi trường Java (JDK 8 đến JDK 25+).

---

### 1. Phạm Vi Công Việc Của Thành Viên 2 (TV2)
- **Use Case chính:** `UC02 – Thanh toán hóa đơn`
- **Công việc bổ sung:** `Quản lý các trạng thái hóa đơn và thanh toán`
- **Các quy tắc nghiệp vụ đảm bảo:**
  - **BR04:** Hóa đơn chỉ được hoàn tất khi thanh toán thành công (`DA_THANH_TOAN`).
  - **BR05:** Thanh toán phải gắn với một hóa đơn cụ thể (`MaHD`).
  - **BR10:** Số lượng tồn kho giảm khi hóa đơn được thanh toán và hoàn tất (trừ trực tiếp vào `LoThuoc` theo lô).
  - **Quản lý trạng thái (State Machine):**
    - `CHO_THANH_TOAN` $\rightarrow$ `DA_THANH_TOAN` (khi thanh toán thành công).
    - `CHO_THANH_TOAN` $\rightarrow$ `DA_HUY` (khi khách hủy mua).
    - Ngăn chặn thanh toán lặp lại hoặc hủy hóa đơn đã hoàn tất.

---

### 2. Cấu Trúc Dự Án (Chuẩn 3-Layer Architecture)

```
c:\Users\kn200\HTTT\
├── run.bat                     # File kích hoạt 1-click biên dịch và chạy GUI Swing
├── test.bat                    # File chạy 6 bài test kiểm thử tự động BR04, BR05, BR10
├── README.md                   # Hướng dẫn tổng quan
├── src/main/java/com/longchau/
│   ├── model/                  # Tầng Thực thể (Entities & Enums)
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
│   │   ├── Database.java       # Seed Data thực tế nhà thuốc Long Châu & Transaction Atomic
│   │   ├── HoaDonDAO.java      # Truy vấn & cập nhật trạng thái Hóa đơn
│   │   ├── ThanhToanDAO.java   # Lưu bản ghi giao dịch thanh toán
│   │   └── LoThuocDAO.java     # Kiểm tra và trừ tồn kho theo từng lô (BR10)
│   ├── service/                # Tầng Xử lý nghiệp vụ (Business Service)
│   │   ├── PaymentException.java
│   │   ├── PaymentRequest.java
│   │   ├── PaymentResult.java
│   │   └── PaymentService.java # Hiện thực hóa UC02 & Quản lý trạng thái
│   ├── view/                   # Tầng Giao diện (Java Swing GUI)
│   │   ├── PaymentManagementFrame.java # Màn hình thanh toán chuẩn Long Châu
│   │   └── ReceiptDialog.java          # Modal xem trước và in biên lai / hóa đơn bán lẻ
│   ├── Main.java               # Điểm khởi chạy ứng dụng GUI
│   └── TestPaymentRunner.java  # Bộ test tự động các trường hợp nghiệp vụ
└── docs/
    └── BAO_CAO_TV2_UC02.md     # Tài liệu đặc tả 5 phần theo chuẩn báo cáo PTIT
```

---

### 3. Hướng Dẫn Biên Dịch & Chạy Ứng Dụng

#### Cách 1: Click đúp chuột (Khuyên dùng)
- Click đúp vào file `run.bat` để biên dịch và mở giao diện quản lý thanh toán.
- Click đúp vào file `test.bat` để chạy bộ test tự động.

#### Cách 2: Sử dụng dòng lệnh Terminal
```powershell
# 1. Biên dịch toàn bộ mã nguồn
& "$env:USERPROFILE\.jdks\openjdk-25.0.1\bin\javac.exe" -encoding UTF-8 -d bin (Get-ChildItem -Recurse -Filter *.java src).FullName

# 2. Khởi chạy giao diện Swing
& "$env:USERPROFILE\.jdks\openjdk-25.0.1\bin\java.exe" "-Dfile.encoding=UTF-8" -cp bin com.longchau.Main

# 3. Chạy kiểm thử tự động
& "$env:USERPROFILE\.jdks\openjdk-25.0.1\bin\java.exe" "-Dfile.encoding=UTF-8" -cp bin com.longchau.TestPaymentRunner
```

