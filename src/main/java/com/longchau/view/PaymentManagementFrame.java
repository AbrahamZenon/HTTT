package com.longchau.view;

import com.longchau.dao.Database;
import com.longchau.model.*;
import com.longchau.service.PaymentException;
import com.longchau.service.PaymentRequest;
import com.longchau.service.PaymentResult;
import com.longchau.service.PaymentService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Giao diện Quản lý Thanh toán Hóa đơn & Trạng thái (TV2)
 * Thiết kế cho Nhà thuốc FPT Long Châu
 */
public class PaymentManagementFrame extends JFrame {
    private final PaymentService paymentService = new PaymentService();
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    // Các thành phần giao diện
    private JComboBox<String> cboFilterStatus;
    private JTable tblInvoices;
    private DefaultTableModel modelInvoices;

    private JLabel lblMaHD;
    private JLabel lblNgayLap;
    private JLabel lblNhanVien;
    private JLabel lblTrangThaiBadge;

    private JTable tblItems;
    private DefaultTableModel modelItems;

    private JLabel lblTongTienVal;
    private JTabbedPane tabPaymentMethods;

    // Tab Tiền mặt
    private JTextField txtTienKhachDua;
    private JLabel lblTienThuaVal;

    // Tab Chuyển khoản QR
    private JLabel lblQrSoTien;
    private JLabel lblQrNoiDung;

    // Tab Thẻ POS
    private JTextField txtPosRef;

    // Action buttons
    private JButton btnPayConfirm;
    private JButton btnCancelInvoice;
    private JButton btnPrintReceipt;

    // KPI / Stats Labels
    private JLabel lblStatPending;
    private JLabel lblStatPaid;
    private JLabel lblStatRevenue;

    private HoaDon selectedHoaDon;

    public PaymentManagementFrame() {
        setTitle("Nhà Thuốc FPT Long Châu - Phân hệ TV2: UC02 Thanh Toán Hóa Đơn");
        setSize(1180, 720);
        setMinimumSize(new Dimension(980, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initComponents();
        loadKPIs();
        loadInvoiceList();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // 1. TOP HEADER & KPI
        JPanel topContainer = new JPanel(new BorderLayout());

        // Header Title
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(0, 82, 204));
        headerPanel.setBorder(new EmptyBorder(12, 20, 12, 20));

        JLabel lblTitle = new JLabel("NHÀ THUỐC FPT LONG CHÂU - QUẢN LÝ THANH TOÁN (UC02)");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTitle.setForeground(Color.WHITE);

        JLabel lblUser = new JLabel("Thu ngân: Nguyễn Văn An (NV01) | Chi nhánh Hà Nội 01");
        lblUser.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblUser.setForeground(new Color(222, 235, 255));

        headerPanel.add(lblTitle, BorderLayout.WEST);
        headerPanel.add(lblUser, BorderLayout.EAST);
        topContainer.add(headerPanel, BorderLayout.NORTH);

        // KPI Bar
        JPanel kpiBar = new JPanel(new GridLayout(1, 4, 15, 0));
        kpiBar.setBackground(new Color(244, 245, 247));
        kpiBar.setBorder(new EmptyBorder(10, 20, 10, 20));

        lblStatPending = createKpiCard(kpiBar, "Chờ thanh toán", "0 HĐ", new Color(143, 96, 0), new Color(255, 240, 179));
        lblStatPaid = createKpiCard(kpiBar, "Đã hoàn tất", "0 HĐ", new Color(0, 102, 68), new Color(227, 252, 239));
        lblStatRevenue = createKpiCard(kpiBar, "Doanh thu ca", "0 đ", new Color(0, 82, 204), new Color(222, 235, 255));

        // Nút reset dữ liệu
        JPanel pnlReset = new JPanel(new GridBagLayout());
        pnlReset.setBackground(Color.WHITE);
        pnlReset.setBorder(BorderFactory.createLineBorder(new Color(223, 225, 230)));
        JButton btnReset = new JButton("Khôi Phục Data Mẫu");
        btnReset.setFocusPainted(false);
        btnReset.setBackground(new Color(244, 245, 247));
        btnReset.addActionListener(e -> {
            Database.getInstance().resetData();
            loadKPIs();
            loadInvoiceList();
            JOptionPane.showMessageDialog(this, "Đã nạp lại dữ liệu mẫu Long Châu thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        });
        pnlReset.add(btnReset);
        kpiBar.add(pnlReset);

        topContainer.add(kpiBar, BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);

        // 2. MAIN SPLIT PANE: LEFT (INVOICES LIST), RIGHT (PAYMENT DETAIL)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(420);
        splitPane.setContinuousLayout(true);

        // --- CỘT TRÁI: DANH SÁCH HÓA ĐƠN ---
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBorder(new EmptyBorder(10, 15, 10, 10));

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Lọc trạng thái:"));
        cboFilterStatus = new JComboBox<>(new String[]{"Chờ thanh toán", "Tất cả", "Đã thanh toán", "Đã hủy"});
        cboFilterStatus.addActionListener(e -> loadInvoiceList());
        filterPanel.add(cboFilterStatus);
        leftPanel.add(filterPanel, BorderLayout.NORTH);

        modelInvoices = new DefaultTableModel(new String[]{"Mã HĐ", "Ngày Lập", "NV Lập", "Tổng Tiền", "Trạng Thái"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblInvoices = new JTable(modelInvoices);
        tblInvoices.setRowHeight(26);
        tblInvoices.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblInvoices.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = tblInvoices.getSelectedRow();
                if (row >= 0) {
                    String maHD = (String) modelInvoices.getValueAt(row, 0);
                    showInvoiceDetail(maHD);
                }
            }
        });
        leftPanel.add(new JScrollPane(tblInvoices), BorderLayout.CENTER);
        splitPane.setLeftComponent(leftPanel);

        // --- CỘT PHẢI: CHI TIẾT & XỬ LÝ THANH TOÁN ---
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setBorder(new EmptyBorder(10, 10, 10, 15));

        // Invoice Header Info
        JPanel invoiceInfoPanel = new JPanel(new GridLayout(2, 2, 8, 4));
        invoiceInfoPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0, 82, 204), 1),
                "Thông Tin Hóa Đơn Được Chọn", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12), new Color(0, 82, 204)));

        lblMaHD = new JLabel("Mã HĐ: --");
        lblMaHD.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblNgayLap = new JLabel("Ngày lập: --");
        lblNhanVien = new JLabel("NV lập: --");
        lblTrangThaiBadge = new JLabel("Trạng thái: --");
        lblTrangThaiBadge.setFont(new Font("Segoe UI", Font.BOLD, 13));

        invoiceInfoPanel.add(lblMaHD);
        invoiceInfoPanel.add(lblTrangThaiBadge);
        invoiceInfoPanel.add(lblNgayLap);
        invoiceInfoPanel.add(lblNhanVien);

        // Bảng Chi tiết danh sách thuốc
        modelItems = new DefaultTableModel(new String[]{"#", "Tên Thuốc", "ĐVT", "Mã Lô", "Hạn Dùng", "Đơn Giá", "SL", "Thành Tiền"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblItems = new JTable(modelItems);
        tblItems.setRowHeight(24);

        // Căn phải cho số tiền
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        tblItems.getColumnModel().getColumn(5).setCellRenderer(rightRenderer);
        tblItems.getColumnModel().getColumn(6).setCellRenderer(rightRenderer);
        tblItems.getColumnModel().getColumn(7).setCellRenderer(rightRenderer);

        JScrollPane scrollItems = new JScrollPane(tblItems);
        scrollItems.setPreferredSize(new Dimension(500, 180));
        scrollItems.setBorder(BorderFactory.createTitledBorder("Danh sách thuốc & Lô thuốc trong đơn"));

        // Khung Giữa Right Panel
        JPanel centerRight = new JPanel(new BorderLayout(5, 5));
        centerRight.add(invoiceInfoPanel, BorderLayout.NORTH);
        centerRight.add(scrollItems, BorderLayout.CENTER);

        // Khung Tổng tiền & Thanh toán
        JPanel paymentContainer = new JPanel(new BorderLayout(5, 5));

        // Dòng tổng tiền
        JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 6));
        totalPanel.setBackground(new Color(250, 251, 252));
        totalPanel.setBorder(BorderFactory.createLineBorder(new Color(223, 225, 230)));
        JLabel lblTongTienTitle = new JLabel("TỔNG TIỀN THANH TOÁN:");
        lblTongTienTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTongTienVal = new JLabel("0 đ");
        lblTongTienVal.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTongTienVal.setForeground(new Color(222, 53, 11));
        totalPanel.add(lblTongTienTitle);
        totalPanel.add(lblTongTienVal);
        paymentContainer.add(totalPanel, BorderLayout.NORTH);

        // Tabs Phương thức thanh toán
        tabPaymentMethods = new JTabbedPane();

        // 1. Tab Tiền mặt
        JPanel tabCash = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        tabCash.add(new JLabel("Tiền khách đưa (VNĐ):"), gbc);

        gbc.gridx = 1; gbc.gridy = 0;
        txtTienKhachDua = new JTextField(12);
        txtTienKhachDua.setFont(new Font("Segoe UI", Font.BOLD, 14));
        txtTienKhachDua.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                calculateChange();
            }
        });
        tabCash.add(txtTienKhachDua, gbc);

        // Nút mệnh giá nhanh
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        JPanel quickPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton btnExact = new JButton("Vừa đủ");
        JButton btn100 = new JButton("100.000");
        JButton btn200 = new JButton("200.000");
        JButton btn500 = new JButton("500.000");
        btnExact.addActionListener(e -> {
            if (selectedHoaDon != null) {
                txtTienKhachDua.setText(String.format(Locale.US, "%.0f", selectedHoaDon.getTongTien()));
                calculateChange();
            }
        });
        btn100.addActionListener(e -> { txtTienKhachDua.setText("100000"); calculateChange(); });
        btn200.addActionListener(e -> { txtTienKhachDua.setText("200000"); calculateChange(); });
        btn500.addActionListener(e -> { txtTienKhachDua.setText("500000"); calculateChange(); });
        quickPanel.add(new JLabel("Nhanh:"));
        quickPanel.add(btnExact);
        quickPanel.add(btn100);
        quickPanel.add(btn200);
        quickPanel.add(btn500);
        tabCash.add(quickPanel, gbc);

        // Tiền thối lại
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        tabCash.add(new JLabel("Tiền thừa trả khách:"), gbc);

        gbc.gridx = 1; gbc.gridy = 2;
        lblTienThuaVal = new JLabel("0 đ");
        lblTienThuaVal.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTienThuaVal.setForeground(new Color(0, 102, 68));
        tabCash.add(lblTienThuaVal, gbc);

        tabPaymentMethods.addTab("Tiền Mặt Tại Quầy", tabCash);

        // 2. Tab Chuyển khoản QR
        JPanel tabQr = new JPanel(new GridLayout(4, 1, 4, 4));
        tabQr.setBorder(new EmptyBorder(8, 12, 8, 12));
        tabQr.add(new JLabel("<html><strong>Ngân hàng:</strong> MB Bank - STK: <strong>0988889999</strong></html>"));
        tabQr.add(new JLabel("<html><strong>Chủ TK:</strong> CTY CP DUOC PHAM FPT LONG CHAU</html>"));
        lblQrSoTien = new JLabel("Số tiền: 0 đ");
        lblQrNoiDung = new JLabel("Nội dung CK: LONGCHAU");
        tabQr.add(lblQrSoTien);
        tabQr.add(lblQrNoiDung);
        tabPaymentMethods.addTab("Chuyển Khoản VietQR", tabQr);

        // 3. Tab Thẻ POS
        JPanel tabPos = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 15));
        tabPos.add(new JLabel("Quẹt thẻ qua máy POS ngân hàng liên kết."));
        tabPos.add(new JLabel("Mã chuẩn chi POS:"));
        txtPosRef = new JTextField("POS-LONGCHAU", 12);
        tabPos.add(txtPosRef);
        tabPaymentMethods.addTab("Thẻ POS / ATM", tabPos);

        paymentContainer.add(tabPaymentMethods, BorderLayout.CENTER);

        // Thanh nút bấm thao tác
        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));

        btnCancelInvoice = new JButton("Hủy Hóa Đơn");
        btnCancelInvoice.setForeground(new Color(222, 53, 11));
        btnCancelInvoice.addActionListener(e -> handleCancelInvoice());

        btnPrintReceipt = new JButton("In Hóa Đơn / Biên Lai");
        btnPrintReceipt.setEnabled(false);
        btnPrintReceipt.addActionListener(e -> {
            if (selectedHoaDon != null) {
                new ReceiptDialog(this, selectedHoaDon, getEnteredCash(), getCalculatedChange()).setVisible(true);
            }
        });

        btnPayConfirm = new JButton("Xác Nhận Thanh Toán (Hoàn Tất HĐ)");
        btnPayConfirm.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnPayConfirm.setBackground(new Color(0, 135, 90));
        btnPayConfirm.setForeground(Color.WHITE);
        btnPayConfirm.setFocusPainted(false);
        btnPayConfirm.addActionListener(e -> handleProcessPayment());

        actionBar.add(btnCancelInvoice);
        actionBar.add(btnPrintReceipt);
        actionBar.add(btnPayConfirm);
        paymentContainer.add(actionBar, BorderLayout.SOUTH);

        centerRight.add(paymentContainer, BorderLayout.SOUTH);
        rightPanel.add(centerRight, BorderLayout.CENTER);

        splitPane.setRightComponent(rightPanel);
        add(splitPane, BorderLayout.CENTER);
    }

    private JLabel createKpiCard(JPanel parent, String title, String val, Color textCol, Color bgCol) {
        JPanel card = new JPanel(new GridLayout(2, 1, 2, 2));
        card.setBackground(bgCol);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(223, 225, 230), 1),
                new EmptyBorder(6, 12, 6, 12)));

        JLabel lblVal = new JLabel(val);
        lblVal.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblVal.setForeground(textCol);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblTitle.setForeground(new Color(107, 119, 140));

        card.add(lblVal);
        card.add(lblTitle);
        parent.add(card);
        return lblVal;
    }

    private void loadKPIs() {
        Map<String, Object> summary = paymentService.getPaymentSummary();
        lblStatPending.setText(summary.get("countPending") + " HĐ");
        lblStatPaid.setText(summary.get("countPaid") + " HĐ");
        double rev = (double) summary.get("totalRevenue");
        lblStatRevenue.setText(currencyFormatter.format(rev));
    }

    private void loadInvoiceList() {
        modelInvoices.setRowCount(0);
        String selectedFilter = (String) cboFilterStatus.getSelectedItem();
        TrangThaiHoaDon status = null;
        if ("Chờ thanh toán".equals(selectedFilter)) status = TrangThaiHoaDon.CHO_THANH_TOAN;
        else if ("Đã thanh toán".equals(selectedFilter)) status = TrangThaiHoaDon.DA_THANH_TOAN;
        else if ("Đã hủy".equals(selectedFilter)) status = TrangThaiHoaDon.DA_HUY;

        List<HoaDon> list = paymentService.getInvoicesByStatus(status);
        for (HoaDon hd : list) {
            modelInvoices.addRow(new Object[]{
                    hd.getMaHD(),
                    hd.getNgayLap(),
                    hd.getMaNV(),
                    currencyFormatter.format(hd.getTongTien()),
                    hd.getTrangThai().getMoTa()
            });
        }

        if (list.size() > 0) {
            tblInvoices.setRowSelectionInterval(0, 0);
        } else {
            clearInvoiceDetail();
        }
    }

    private void showInvoiceDetail(String maHD) {
        selectedHoaDon = paymentService.getInvoiceDetail(maHD);
        if (selectedHoaDon == null) return;

        lblMaHD.setText("Mã HĐ: " + selectedHoaDon.getMaHD());
        lblNgayLap.setText("Ngày lập: " + selectedHoaDon.getNgayLap());
        lblNhanVien.setText("NV lập: " + selectedHoaDon.getMaNV());
        lblTrangThaiBadge.setText("Trạng thái: " + selectedHoaDon.getTrangThai().getMoTa());

        if (selectedHoaDon.getTrangThai() == TrangThaiHoaDon.CHO_THANH_TOAN) {
            lblTrangThaiBadge.setForeground(new Color(143, 96, 0));
            btnPayConfirm.setEnabled(true);
            btnCancelInvoice.setEnabled(true);
            btnPrintReceipt.setEnabled(false);
            tabPaymentMethods.setEnabled(true);
        } else if (selectedHoaDon.getTrangThai() == TrangThaiHoaDon.DA_THANH_TOAN) {
            lblTrangThaiBadge.setForeground(new Color(0, 102, 68));
            btnPayConfirm.setEnabled(false);
            btnCancelInvoice.setEnabled(false);
            btnPrintReceipt.setEnabled(true);
            tabPaymentMethods.setEnabled(false);
        } else {
            lblTrangThaiBadge.setForeground(new Color(191, 38, 0));
            btnPayConfirm.setEnabled(false);
            btnCancelInvoice.setEnabled(false);
            btnPrintReceipt.setEnabled(false);
            tabPaymentMethods.setEnabled(false);
        }

        // Bảng chi tiết thuốc
        modelItems.setRowCount(0);
        int idx = 1;
        for (ChiTietHoaDon ct : selectedHoaDon.getChiTiet()) {
            modelItems.addRow(new Object[]{
                    idx++,
                    ct.getTenThuoc() != null ? ct.getTenThuoc() : ct.getMaThuoc(),
                    ct.getDonViTinh() != null ? ct.getDonViTinh() : "--",
                    ct.getMaLo(),
                    ct.getHanSuDung() != null ? ct.getHanSuDung() : "--",
                    currencyFormatter.format(ct.getDonGia()),
                    ct.getSoLuong(),
                    currencyFormatter.format(ct.getThanhTien())
            });
        }

        lblTongTienVal.setText(currencyFormatter.format(selectedHoaDon.getTongTien()));
        txtTienKhachDua.setText(String.format(Locale.US, "%.0f", selectedHoaDon.getTongTien()));
        calculateChange();

        lblQrSoTien.setText("Số tiền: " + currencyFormatter.format(selectedHoaDon.getTongTien()));
        lblQrNoiDung.setText("Nội dung CK: LONGCHAU " + selectedHoaDon.getMaHD());
    }

    private void clearInvoiceDetail() {
        selectedHoaDon = null;
        lblMaHD.setText("Mã HĐ: --");
        lblNgayLap.setText("Ngày lập: --");
        lblNhanVien.setText("NV lập: --");
        lblTrangThaiBadge.setText("Trạng thái: --");
        modelItems.setRowCount(0);
        lblTongTienVal.setText("0 đ");
        txtTienKhachDua.setText("");
        lblTienThuaVal.setText("0 đ");
        btnPayConfirm.setEnabled(false);
        btnCancelInvoice.setEnabled(false);
        btnPrintReceipt.setEnabled(false);
    }

    private void calculateChange() {
        if (selectedHoaDon == null) return;
        double tongTien = selectedHoaDon.getTongTien();
        double entered = getEnteredCash();
        double change = entered - tongTien;

        if (change >= 0) {
            lblTienThuaVal.setText(currencyFormatter.format(change));
            lblTienThuaVal.setForeground(new Color(0, 102, 68));
        } else {
            lblTienThuaVal.setText("Còn thiếu: " + currencyFormatter.format(Math.abs(change)));
            lblTienThuaVal.setForeground(new Color(222, 53, 11));
        }
    }

    private double getEnteredCash() {
        try {
            return Double.parseDouble(txtTienKhachDua.getText().trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double getCalculatedChange() {
        if (selectedHoaDon == null) return 0.0;
        double entered = getEnteredCash();
        return Math.max(0.0, entered - selectedHoaDon.getTongTien());
    }

    /**
     * Xử lý xác nhận thanh toán (Hiện thực hóa UC02)
     */
    private void handleProcessPayment() {
        if (selectedHoaDon == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn hóa đơn để thanh toán!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int selectedTab = tabPaymentMethods.getSelectedIndex();
        PhuongThucThanhToan pt = PhuongThucThanhToan.TIEN_MAT;
        double soTienKhachTra = selectedHoaDon.getTongTien();
        String ref = null;

        if (selectedTab == 0) {
            pt = PhuongThucThanhToan.TIEN_MAT;
            soTienKhachTra = getEnteredCash();
            if (soTienKhachTra < selectedHoaDon.getTongTien()) {
                JOptionPane.showMessageDialog(this, "Số tiền khách đưa không đủ để hoàn tất thanh toán!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else if (selectedTab == 1) {
            pt = PhuongThucThanhToan.CHUYEN_KHOAN;
            ref = "QR-" + selectedHoaDon.getMaHD();
        } else if (selectedTab == 2) {
            pt = PhuongThucThanhToan.THE;
            ref = txtPosRef.getText().trim();
        }

        PaymentRequest req = new PaymentRequest(selectedHoaDon.getMaHD(), pt, soTienKhachTra, "NV01");
        req.setMaGiaoDichRef(ref);

        try {
            PaymentResult result = paymentService.processPayment(req);
            if (result.isSuccess()) {
                loadKPIs();
                loadInvoiceList();
                showInvoiceDetail(selectedHoaDon.getMaHD());

                // Mở biên lai in
                ReceiptDialog receipt = new ReceiptDialog(this, selectedHoaDon, soTienKhachTra, result.getTienThua());
                receipt.setVisible(true);
            }
        } catch (PaymentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi nghiệp vụ thanh toán", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Hủy hóa đơn (Quản lý trạng thái của TV2)
     */
    private void handleCancelInvoice() {
        if (selectedHoaDon == null) return;

        String reason = JOptionPane.showInputDialog(this,
                "Nhập lý do hủy hóa đơn " + selectedHoaDon.getMaHD() + ":",
                "Xác nhận hủy hóa đơn",
                JOptionPane.QUESTION_MESSAGE);

        if (reason != null && !reason.trim().isEmpty()) {
            try {
                paymentService.cancelInvoice(selectedHoaDon.getMaHD(), reason.trim());
                JOptionPane.showMessageDialog(this, "Đã hủy hóa đơn " + selectedHoaDon.getMaHD() + " thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                loadKPIs();
                loadInvoiceList();
                showInvoiceDetail(selectedHoaDon.getMaHD());
            } catch (PaymentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

