# BAO CAO PHAN TICH VA THIET KE PHAN HE THANH VIEN 2 (TV2)
## De tai: He Thong Quan Ly Ban Hang Va Ton Kho Nha Thuoc Long Chau
**Hoc phan:** Phan tich & Thiet ke He thong Thong tin (Chuan mau bao cao PTIT)  
**Thanh vien phu trach:** Thanh vien 2 (TV2)  
**Use Case trong tam:** `UC02 - Thanh toan hoa don`  
**Nhiem vu bo sung:** `Quan ly cac trang thai hoa don va thanh toan`  
**Ngon ngu trien khai:** `Java (JDK 8 - JDK 25+)` / `Java Swing UI`

---

## 1. Dac ta Use Case chi tiet (UC02 - Thanh toan hoa don)

### 1.1. Bang dac ta Use Case
- **Ma Use Case:** UC02
- **Ten Use Case:** Thanh toan hoa don
- **Tac nhan chinh:** Nhan vien ban hang (Thu ngan)
- **Tac nhan phu:** Khach hang
- **Tien dieu kien:** Hoa don da duoc lap (boi TV1) o trang thai `CHO_THANH_TOAN`. Khach hang san sang thanh toan.
- **Hau dieu kien:**
  - Hoa don chuyen sang trang thai `DA_THANH_TOAN` (BR04).
  - Ban ghi `ThanhToan` duoc tao lien ket voi hoa don (BR05).
  - Ton kho cua cac `LoThuoc` trong hoa don bi tru chinh xac (BR10).
  - Bien lai/hoa don ban le duoc in ra cho khach.

---

## 2. Quan ly trang thai (State Machine)
- `HoaDon`: `CHO_THANH_TOAN` -> `DA_THANH_TOAN` (khi thanh toan thanh cong) hoac `DA_HUY` (khi khach huy mua).
- `ThanhToan`: `DANG_XU_LY` -> `THANH_CONG` hoac `THAT_BAI`.
- Khong cho phep thanh toan lai hoa don da hoan tat hoac da huy. Khong cho phep huy hoa don da thanh toan.

---

## 3. Cau truc ma nguon Java

```
c:\Users\kn200\HTTT\
|-- run.bat                     # File kich hoat 1-click chay giao dien Swing
|-- test.bat                    # File chay 6 bai kiem thu tu dong BR04, BR05, BR10
|-- src/main/java/com/longchau/
    |-- model/                  # Tang Entity & Enums
    |   |-- TrangThaiHoaDon.java
    |   |-- TrangThaiThanhToan.java
    |   |-- PhuongThucThanhToan.java
    |   |-- NhanVien.java
    |   |-- Thuoc.java
    |   |-- LoThuoc.java
    |   |-- ChiTietHoaDon.java
    |   |-- ThanhToan.java
    |   |-- HoaDon.java
    |-- dao/                    # Tang Truy xuat du lieu (Data Access Object)
    |   |-- Database.java       # Mock DB quan he voi Seed Data thuc te Long Chau
    |   |-- HoaDonDAO.java      # Cap nhat & tra cuu Hoa don
    |   |-- ThanhToanDAO.java   # Luu giao dich thanh toan
    |   |-- LoThuocDAO.java     # Kiem tra & tru ton kho theo lo (BR10)
    |-- service/                # Tang Xu ly nghiep vu (Business Logic)
    |   |-- PaymentException.java
    |   |-- PaymentRequest.java
    |   |-- PaymentResult.java
    |   |-- PaymentService.java # Kiem tra BR04, BR05, BR10 & State Machine
    |-- view/                   # Tang Giao dien (Java Swing GUI)
    |   |-- PaymentManagementFrame.java # Man hinh quay thu ngan Long Chau
    |   |-- ReceiptDialog.java          # Modal in bien lai / hoa don ban le
    |-- Main.java               # Khoi chay ung dung GUI
    |-- TestPaymentRunner.java  # Kiem thu tu dong 6 ca kiem thu
```

---

## 4. Huong dan chay chuong trinh
1. Nhap dup chuot vao file `run.bat` de chay giao dien Java Swing.
2. Nhap dup chuot vao file `test.bat` de chay bo kiem thu tu dong 6/6 test cases.
