package com.longchau.service;

import com.longchau.dao.HoaDonDAO;
import com.longchau.dao.LoThuocDAO;
import com.longchau.dao.ThanhToanDAO;
import com.longchau.model.*;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Service xử lý nghiệp vụ Quản lý Hóa đơn & Thanh toán (TV2)
 * Hiện thực hóa các quy tắc:
 *   - BR04: Hóa đơn chỉ được hoàn tất khi thanh toán thành công.
 *   - BR05: Thanh toán phải gắn với một hóa đơn cụ thể.
 *   - BR10: Số lượng tồn kho giảm khi hóa đơn được thanh toán và hoàn tất.
 *   - State Machine: Quản lý vòng đời trạng thái hóa đơn (CHO_THANH_TOAN -> DA_THANH_TOAN / DA_HUY).
 */
public class PaymentService {
    private final HoaDonDAO hoaDonDAO = new HoaDonDAO();
    private final ThanhToanDAO thanhToanDAO = new ThanhToanDAO();
    private final LoThuocDAO loThuocDAO = new LoThuocDAO();

    public List<HoaDon> getAllInvoices() {
        return hoaDonDAO.findAll();
    }

    public List<HoaDon> getInvoicesByStatus(TrangThaiHoaDon status) {
        if (status == null) {
            return getAllInvoices();
        }
        return hoaDonDAO.findByStatus(status);
    }

    public HoaDon getInvoiceDetail(String maHD) {
        return hoaDonDAO.findById(maHD);
    }

    /**
     * Hiện thực hóa UC02 - Thanh toán hóa đơn:
     * Đồng bộ hóa, kiểm tra trạng thái và cập nhật trừ kho theo lô (BR10)
     */
    public synchronized PaymentResult processPayment(PaymentRequest req) throws PaymentException {
        if (req == null || req.getMaHD() == null || req.getMaHD().trim().isEmpty()) {
            throw new PaymentException("Mã hóa đơn không được để trống!");
        }

        // Bước 1: Kiểm tra sự tồn tại của Hóa đơn
        HoaDon hd = hoaDonDAO.findById(req.getMaHD());
        if (hd == null) {
            throw new PaymentException("Không tìm thấy hóa đơn mã '" + req.getMaHD() + "'!");
        }

        // Bước 2: Kiểm tra trạng thái hiện tại (State Machine check)
        if (hd.getTrangThai() == TrangThaiHoaDon.DA_THANH_TOAN) {
            throw new PaymentException("Hóa đơn '" + req.getMaHD() + "' đã được thanh toán trước đó. Không thể thanh toán lại!");
        }
        if (hd.getTrangThai() == TrangThaiHoaDon.DA_HUY) {
            throw new PaymentException("Hóa đơn '" + req.getMaHD() + "' đã bị hủy. Không thể tiến hành thanh toán!");
        }
        if (hd.getTrangThai() != TrangThaiHoaDon.CHO_THANH_TOAN) {
            throw new PaymentException("Trạng thái hóa đơn không hợp lệ để thanh toán.");
        }

        double tongTien = hd.getTongTien();
        double tienThua = 0.0;
        String maGiaoDich = req.getMaGiaoDichRef();

        // Bước 3: Kiểm tra phương thức thanh toán và số tiền
        if (req.getPhuongThuc() == null) {
            throw new PaymentException("Chưa chọn phương thức thanh toán!");
        }

        if (req.getPhuongThuc() == PhuongThucThanhToan.TIEN_MAT) {
            if (req.getSoTienKhachTra() < tongTien) {
                String msg = String.format("Số tiền khách đưa (%,.0f đ) không đủ để thanh toán tổng tiền (%,.0f đ)!",
                        req.getSoTienKhachTra(), tongTien);
                throw new PaymentException(msg);
            }
            tienThua = req.getSoTienKhachTra() - tongTien;
            if (maGiaoDich == null || maGiaoDich.trim().isEmpty()) {
                maGiaoDich = "CASH-" + System.currentTimeMillis() % 1000000;
            }
        } else if (req.getPhuongThuc() == PhuongThucThanhToan.CHUYEN_KHOAN) {
            if (maGiaoDich == null || maGiaoDich.trim().isEmpty()) {
                maGiaoDich = "QR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            }
        } else if (req.getPhuongThuc() == PhuongThucThanhToan.THE) {
            if (maGiaoDich == null || maGiaoDich.trim().isEmpty()) {
                maGiaoDich = "POS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            }
        }

        // Bước 4: Kiểm tra tồn kho của tất cả các lô thuốc TRƯỚC KHI TRỪ (BR10 & BR03)
        for (ChiTietHoaDon item : hd.getChiTiet()) {
            LoThuoc lt = loThuocDAO.find(item.getMaLo(), item.getMaThuoc());
            if (lt == null) {
                throw new PaymentException("Không tìm thấy thông tin lô thuốc '" + item.getMaLo() + "' của thuốc " + item.getMaThuoc());
            }
            if (lt.getSoLuongTon() < item.getSoLuong()) {
                throw new PaymentException(String.format(
                        "Lỗi tồn kho: Thuốc '%s' (Lô: %s) chỉ còn tồn %d, không đủ để bán %d!",
                        item.getTenThuoc() != null ? item.getTenThuoc() : item.getMaThuoc(),
                        item.getMaLo(),
                        lt.getSoLuongTon(),
                        item.getSoLuong()
                ));
            }
        }

        // Bước 5: Bắt đầu giao dịch cập nhật (Atomic Update)
        // 5.1 Giảm tồn kho theo từng lô thuốc (BR10)
        for (ChiTietHoaDon item : hd.getChiTiet()) {
            loThuocDAO.decreaseStock(item.getMaLo(), item.getMaThuoc(), item.getSoLuong());
        }

        // 5.2 Tạo bản ghi Thanh toán gắn với hóa đơn (BR05)
        String maTT = "TT" + new SimpleDateFormat("yyMMdd").format(new Date()) + "-" +
                UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String ngayTao = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        ThanhToan thanhToan = new ThanhToan(
                maTT,
                hd.getMaHD(),
                tongTien,
                req.getPhuongThuc(),
                TrangThaiThanhToan.THANH_CONG,
                ngayTao,
                maGiaoDich,
                req.getGhiChu() != null ? req.getGhiChu() : "Thanh toán thành công bởi " + req.getMaNV()
        );
        thanhToanDAO.insert(thanhToan);

        // 5.3 Cập nhật trạng thái Hóa đơn sang DA_THANH_TOAN (BR04)
        hoaDonDAO.updateStatus(hd.getMaHD(), TrangThaiHoaDon.DA_THANH_TOAN);

        return new PaymentResult(true, "Thanh toán thành công và đã cập nhật tồn kho theo lô!",
                thanhToan, tienThua, TrangThaiHoaDon.DA_THANH_TOAN);
    }

    /**
     * Hủy hóa đơn (Quản lý trạng thái hóa đơn của TV2):
     * Chỉ cho phép hủy hóa đơn đang ở trạng thái CHO_THANH_TOAN.
     */
    public synchronized void cancelInvoice(String maHD, String reason) throws PaymentException {
        if (maHD == null || maHD.trim().isEmpty()) {
            throw new PaymentException("Mã hóa đơn không hợp lệ!");
        }
        HoaDon hd = hoaDonDAO.findById(maHD);
        if (hd == null) {
            throw new PaymentException("Không tìm thấy hóa đơn mã '" + maHD + "'!");
        }
        if (hd.getTrangThai() == TrangThaiHoaDon.DA_THANH_TOAN) {
            throw new PaymentException("Không thể hủy hóa đơn đã thanh toán hoàn tất!");
        }
        if (hd.getTrangThai() == TrangThaiHoaDon.DA_HUY) {
            throw new PaymentException("Hóa đơn này đã được hủy trước đó!");
        }

        hoaDonDAO.updateStatus(maHD, TrangThaiHoaDon.DA_HUY);
        hoaDonDAO.updateGhiChu(maHD, "[Đã hủy: " + (reason != null ? reason : "Khách yêu cầu") + "]");
    }

    /**
     * Thống kê nhanh phục vụ báo cáo TV2
     */
    public Map<String, Object> getPaymentSummary() {
        Map<String, Object> summary = new HashMap<>();
        int countPending = 0;
        int countPaid = 0;
        int countCancelled = 0;
        double totalRevenue = 0.0;

        for (HoaDon hd : hoaDonDAO.findAll()) {
            if (hd.getTrangThai() == TrangThaiHoaDon.CHO_THANH_TOAN) countPending++;
            else if (hd.getTrangThai() == TrangThaiHoaDon.DA_THANH_TOAN) {
                countPaid++;
                totalRevenue += hd.getTongTien();
            } else if (hd.getTrangThai() == TrangThaiHoaDon.DA_HUY) countCancelled++;
        }

        summary.put("countPending", countPending);
        summary.put("countPaid", countPaid);
        summary.put("countCancelled", countCancelled);
        summary.put("totalRevenue", totalRevenue);
        return summary;
    }
}
