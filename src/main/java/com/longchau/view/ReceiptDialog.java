package com.longchau.view;

import com.longchau.model.ChiTietHoaDon;
import com.longchau.model.HoaDon;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Hộp thoại hiển thị Hóa đơn bán lẻ & Biên lai thanh toán chuẩn Long Châu
 */
public class ReceiptDialog extends JDialog {
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public ReceiptDialog(Frame parent, HoaDon hoaDon, double tienKhachTra, double tienThua) {
        super(parent, "Biên lai thanh toán - Nhà thuốc FPT Long Châu", true);
        setSize(420, 600);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // Header Biên lai
        JLabel lblBrand = new JLabel("FPT LONG CHÂU");
        lblBrand.setFont(new Font("Monospaced", Font.BOLD, 18));
        lblBrand.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel("HỆ THỐNG NHÀ THUỐC LONG CHÂU");
        lblSub.setFont(new Font("Monospaced", Font.BOLD, 11));
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblAddr = new JLabel("Chi nhánh: 123 Cầu Giấy, Hà Nội - ĐT: 1800 6928");
        lblAddr.setFont(new Font("Monospaced", Font.PLAIN, 10));
        lblAddr.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTitle = new JLabel("HÓA ĐƠN BÁN LẺ & BIÊN LAI");
        lblTitle.setFont(new Font("Monospaced", Font.BOLD, 14));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblDiv1 = new JLabel("----------------------------------------------");
        lblDiv1.setAlignmentX(Component.CENTER_ALIGNMENT);

        mainPanel.add(lblBrand);
        mainPanel.add(lblSub);
        mainPanel.add(lblAddr);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(lblTitle);
        mainPanel.add(lblDiv1);

        // Thông tin HĐ
        JTextArea txtMeta = new JTextArea();
        txtMeta.setFont(new Font("Monospaced", Font.PLAIN, 11));
        txtMeta.setEditable(false);
        txtMeta.setBackground(Color.WHITE);

        String maTT = hoaDon.getThanhToan() != null ? hoaDon.getThanhToan().getMaTT() : "N/A";
        String phuongThuc = hoaDon.getThanhToan() != null ? hoaDon.getThanhToan().getPhuongThuc().getMoTa() : "Tiền mặt";
        String ngayTT = hoaDon.getThanhToan() != null ? hoaDon.getThanhToan().getNgayTao() : hoaDon.getNgayLap();

        txtMeta.setText(
                "Số HĐ   : " + hoaDon.getMaHD() + "\n" +
                "Mã GD   : " + maTT + "\n" +
                "Ngày    : " + ngayTT + "\n" +
                "Thu ngân: " + hoaDon.getMaNV() + "\n" +
                "----------------------------------------------\n"
        );
        mainPanel.add(txtMeta);

        // Danh sách mặt hàng
        StringBuilder sbItems = new StringBuilder();
        sbItems.append(String.format("%-20s %3s %9s %10s\n", "Tên thuốc", "SL", "Đơn giá", "T.Tiền"));
        sbItems.append("----------------------------------------------\n");

        for (ChiTietHoaDon ct : hoaDon.getChiTiet()) {
            String name = ct.getTenThuoc() != null ? ct.getTenThuoc() : ct.getMaThuoc();
            if (name.length() > 20) name = name.substring(0, 18) + "..";
            sbItems.append(String.format("%-20s %3d %9.0f %10.0f\n",
                    name, ct.getSoLuong(), ct.getDonGia(), ct.getThanhTien()));
            sbItems.append(String.format("  [Lô: %s | HSD: %s]\n", ct.getMaLo(), ct.getHanSuDung() != null ? ct.getHanSuDung() : ""));
        }
        sbItems.append("----------------------------------------------\n");
        sbItems.append(String.format("TỔNG TIỀN HÀNG: %28s\n", currencyFormatter.format(hoaDon.getTongTien())));
        sbItems.append(String.format("Phương thức   : %28s\n", phuongThuc));
        if (tienKhachTra > 0) {
            sbItems.append(String.format("Tiền khách đưa: %28s\n", currencyFormatter.format(tienKhachTra)));
            sbItems.append(String.format("Tiền thối lại : %28s\n", currencyFormatter.format(tienThua)));
        }
        sbItems.append("==============================================\n");
        sbItems.append(" Cảm ơn Quý khách & Hẹn gặp lại!\n");
        sbItems.append(" Chúc Quý khách thật nhiều sức khỏe!\n");

        JTextArea txtItems = new JTextArea(sbItems.toString());
        txtItems.setFont(new Font("Monospaced", Font.PLAIN, 11));
        txtItems.setEditable(false);
        txtItems.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(txtItems);
        scrollPane.setBorder(null);
        mainPanel.add(scrollPane);

        add(mainPanel, BorderLayout.CENTER);

        // Nút in và đóng
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnPrint = new JButton("In Biên Lai");
        btnPrint.setBackground(new Color(0, 82, 204));
        btnPrint.setForeground(Color.WHITE);
        btnPrint.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Đã gửi lệnh in biên lai tới máy in nhiệt tại quầy thu ngân!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        });

        JButton btnClose = new JButton("Đóng");
        btnClose.addActionListener(e -> dispose());

        bottomPanel.add(btnPrint);
        bottomPanel.add(btnClose);
        add(bottomPanel, BorderLayout.SOUTH);
    }
}
