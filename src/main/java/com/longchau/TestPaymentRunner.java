package com.longchau;

import com.longchau.dao.Database;
import com.longchau.model.*;
import com.longchau.service.PaymentException;
import com.longchau.service.PaymentRequest;
import com.longchau.service.PaymentResult;
import com.longchau.service.PaymentService;

/**
 * Lớp kiểm thử tự động dòng lệnh các quy tắc nghiệp vụ BR04, BR05, BR10 của TV2
 */
public class TestPaymentRunner {
    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println(" KIỂM THỬ TỰ ĐỘNG CÁC QUY TẮC NGHIỆP VỤ CỦA THÀNH VIÊN 2 (JAVA)");
        System.out.println("===============================================================");

        PaymentService service = new PaymentService();
        int passed = 0;
        int failed = 0;

        // Test 1: Kiểm tra lấy danh sách và chi tiết hóa đơn
        try {
            HoaDon hd = service.getInvoiceDetail("HD261115-001");
            if (hd != null && hd.getTrangThai() == TrangThaiHoaDon.CHO_THANH_TOAN && hd.getChiTiet().size() == 3) {
                System.out.println("[PASS] Test 1: Lấy thông tin hóa đơn chờ thanh toán HD261115-001 thành công.");
                passed++;
            } else {
                System.err.println("[FAIL] Test 1: Thông tin hóa đơn không chính xác.");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("[FAIL] Test 1 ngoại lệ: " + e.getMessage());
            failed++;
        }

        // Test 2: Thanh toán tiền mặt thành công -> Kiểm tra BR04, BR05, BR10
        try {
            String maHD = "HD261115-001";
            double tongTien = 475000.0;
            double tienKhach = 500000.0;

            // Kiểm tra tồn kho trước khi thanh toán của TH01 (LO2026A)
            LoThuoc loTruoc = Database.getInstance().loThuocs.get("LO2026A#TH01");
            int tonTruoc = loTruoc.getSoLuongTon();

            PaymentRequest req = new PaymentRequest(maHD, PhuongThucThanhToan.TIEN_MAT, tienKhach, "NV01");
            PaymentResult res = service.processPayment(req);

            HoaDon hdSau = service.getInvoiceDetail(maHD);
            LoThuoc loSau = Database.getInstance().loThuocs.get("LO2026A#TH01");

            // BR04: Trạng thái HĐ chuyển thành DA_THANH_TOAN
            boolean br04Ok = hdSau.getTrangThai() == TrangThaiHoaDon.DA_THANH_TOAN;
            // BR05: Có bản ghi thanh toán gắn liền với HĐ
            boolean br05Ok = res.getThanhToan() != null && res.getThanhToan().getMaHD().equals(maHD);
            // BR10: Tồn kho giảm đúng 1 đơn vị
            boolean br10Ok = loSau.getSoLuongTon() == (tonTruoc - 1);
            // Tiền thừa đúng 25.000đ
            boolean tienThuaOk = res.getTienThua() == 25000.0;

            if (br04Ok && br05Ok && br10Ok && tienThuaOk) {
                System.out.println("[PASS] Test 2: Thanh toán thành công! BR04 (Đã thanh toán), BR05 (Gắn mã TT), BR10 (Trừ kho từ " + tonTruoc + " xuống " + loSau.getSoLuongTon() + ") đều chuẩn xác!");
                passed++;
            } else {
                System.err.println("[FAIL] Test 2: Vi phạm quy tắc nghiệp vụ BR04, BR05 hoặc BR10.");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("[FAIL] Test 2 ngoại lệ: " + e.getMessage());
            failed++;
        }

        // Test 3: Thanh toán khi tiền khách đưa nhỏ hơn tổng tiền -> Bắt lỗi
        try {
            PaymentRequest reqThieuTien = new PaymentRequest("HD261115-002", PhuongThucThanhToan.TIEN_MAT, 100000.0, "NV01");
            service.processPayment(reqThieuTien);
            System.err.println("[FAIL] Test 3: Lẽ ra phải báo lỗi vì tiền khách đưa không đủ!");
            failed++;
        } catch (PaymentException pe) {
            System.out.println("[PASS] Test 3: Bắt chính xác ngoại lệ khi tiền khách đưa không đủ: " + pe.getMessage());
            passed++;
        }

        // Test 4: Không cho phép thanh toán lại hóa đơn đã thanh toán
        try {
            PaymentRequest reqLapLai = new PaymentRequest("HD261115-000", PhuongThucThanhToan.TIEN_MAT, 135000.0, "NV01");
            service.processPayment(reqLapLai);
            System.err.println("[FAIL] Test 4: Lẽ ra phải chặn thanh toán hóa đơn đã hoàn tất!");
            failed++;
        } catch (PaymentException pe) {
            System.out.println("[PASS] Test 4: Bắt chính xác ngoại lệ khi cố thanh toán hóa đơn đã thanh toán: " + pe.getMessage());
            passed++;
        }

        // Test 5: Quản lý trạng thái - Hủy hóa đơn đang chờ thanh toán
        try {
            service.cancelInvoice("HD261115-002", "Khách đổi ý không mua");
            HoaDon hdHuy = service.getInvoiceDetail("HD261115-002");
            if (hdHuy.getTrangThai() == TrangThaiHoaDon.DA_HUY) {
                System.out.println("[PASS] Test 5: Hủy hóa đơn chờ thanh toán thành công (Trạng thái chuyển sang DA_HUY).");
                passed++;
            } else {
                System.err.println("[FAIL] Test 5: Hóa đơn chưa chuyển sang DA_HUY.");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("[FAIL] Test 5 ngoại lệ: " + e.getMessage());
            failed++;
        }

        // Test 6: Không cho phép hủy hóa đơn đã thanh toán hoàn tất
        try {
            service.cancelInvoice("HD261115-000", "Thử hủy HĐ đã thu tiền");
            System.err.println("[FAIL] Test 6: Lẽ ra không được phép hủy hóa đơn đã thanh toán!");
            failed++;
        } catch (PaymentException pe) {
            System.out.println("[PASS] Test 6: Bắt chính xác lỗi không thể hủy hóa đơn đã thanh toán: " + pe.getMessage());
            passed++;
        }

        System.out.println("===============================================================");
        System.out.println(" KẾT QUẢ KIỂM THỬ: " + passed + " PASSED / " + (passed + failed) + " TOTAL");
        System.out.println("===============================================================");
    }
}

