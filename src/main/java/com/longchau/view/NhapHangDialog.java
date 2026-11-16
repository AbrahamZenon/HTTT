package com.longchau.view;

import com.longchau.dao.Database;
import com.longchau.model.ChiTietPhieuNhap;
import com.longchau.model.NhaCungCap;
import com.longchau.model.PhieuNhap;
import com.longchau.model.Thuoc;
import com.longchau.service.NhapHangService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class NhapHangDialog extends JDialog {
    private static final Locale VI = Locale.forLanguageTag("vi-VN");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private final Database db = Database.getInstance();
    private final NhapHangService service = new NhapHangService();
    private final List<ChiTietPhieuNhap> lines = new ArrayList<>();
    private final List<PhieuNhap> history = new ArrayList<>();
    private final JTextField receiptId = new JTextField();
    private final JTextField document = new JTextField();
    private final JComboBox<NhaCungCap> supplier = new JComboBox<>();
    private final JComboBox<String> employee = new JComboBox<>();
    private final JComboBox<String> product = new JComboBox<>();
    private final JTextField lot = new JTextField();
    private final JTextField expiry = new JTextField();
    private final JTextField quantity = new JTextField();
    private final JTextField cost = new JTextField();
    private final JLabel total = new JLabel();
    private final DefaultTableModel draftModel = lotModel();
    private final JTable draftTable = new JTable(draftModel);
    private final DefaultTableModel historyModel = readOnly("Mã phiếu", "Ngày nhập", "Nhà cung cấp",
            "Nhân viên", "Số chứng từ", "Tổng tiền");
    private final JTable historyTable = new JTable(historyModel);
    private final DefaultTableModel detailModel = lotModel();

    NhapHangDialog(JFrame owner) {
        super(owner, "UC03 - Tiếp nhận và lập phiếu nhập hàng", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        db.nhaCungCaps.values().stream().sorted(Comparator.comparing(NhaCungCap::getMaNCC))
                .forEach(supplier::addItem);
        db.nhanViens.keySet().stream().sorted().forEach(employee::addItem);
        db.thuocs.values().stream().sorted(Comparator.comparing(Thuoc::getMaThuoc))
                .forEach(thuoc -> product.addItem(thuoc.getMaThuoc() + " - " + thuoc.getTenThuoc()));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Lập phiếu nhập", createDraft());
        tabs.addTab("Lịch sử phiếu nhập", createHistory());
        add(tabs);
        refreshDraft();
        refreshHistory();
        setSize(1000, 625);
        setMinimumSize(new Dimension(810, 550));
        setLocationRelativeTo(owner);
    }

    private JPanel createDraft() {
        JPanel panel = paddedPanel();
        JPanel header = new JPanel(new GridLayout(2, 4, 8, 4));
        header.setBorder(BorderFactory.createTitledBorder("Thông tin phiếu"));
        addFields(header, new String[]{"Mã phiếu", "Nhà cung cấp", "Mã nhân viên", "Số chứng từ (nếu có)"},
                receiptId, supplier, employee, document);
        JPanel lineForm = new JPanel(new BorderLayout(0, 5));
        lineForm.setBorder(BorderFactory.createTitledBorder("Lô hàng thực nhận"));
        JPanel fields = new JPanel(new BorderLayout(8, 0));
        JPanel medicine = new JPanel(new GridLayout(2, 1, 0, 4));
        addFields(medicine, new String[]{"Thuốc"}, product);
        fields.add(medicine, BorderLayout.WEST);
        JPanel lotFields = new JPanel(new GridLayout(2, 4, 8, 4));
        addFields(lotFields, new String[]{"Mã lô", "Hạn dùng (YYYY-MM-DD)", "Số lượng đạt", "Đơn giá (đ)"},
                lot, expiry, quantity, cost);
        fields.add(lotFields);
        lineForm.add(fields);
        JButton addLine = new JButton("Thêm lô đạt kiểm tra");
        addLine.addActionListener(event -> addLine());
        JPanel addBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        addBar.add(addLine);
        lineForm.add(addBar, BorderLayout.SOUTH);

        JPanel headerAndLot = new JPanel(new BorderLayout(0, 8));
        headerAndLot.add(header, BorderLayout.NORTH);
        headerAndLot.add(lineForm);
        panel.add(headerAndLot, BorderLayout.NORTH);
        draftTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        draftTable.getColumnModel().getColumn(1).setPreferredWidth(265);
        panel.add(new JScrollPane(draftTable));

        JPanel footer = new JPanel(new BorderLayout());
        JButton remove = new JButton("Bỏ lô đã chọn");
        remove.addActionListener(event -> {
            int index = draftTable.getSelectedRow();
            if (index >= 0) {
                lines.remove(index);
                refreshDraft();
            }
        });
        footer.add(remove, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        JButton save = new JButton("Xác nhận nhập kho");
        save.addActionListener(event -> save());
        actions.add(total);
        actions.add(save);
        footer.add(actions, BorderLayout.EAST);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createHistory() {
        JPanel panel = paddedPanel();
        JTable details = new JTable(detailModel);
        details.getColumnModel().getColumn(1).setPreferredWidth(265);
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyTable.getSelectionModel().addListSelectionListener(event -> {
            detailModel.setRowCount(0);
            int index = historyTable.getSelectedRow();
            if (index >= 0) {
                history.get(index).getChiTiet().forEach(line -> addRow(detailModel, line));
            }
        });
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(historyTable),
                new JScrollPane(details));
        split.setResizeWeight(0.5);
        panel.add(split);
        return panel;
    }

    private void addLine() {
        String selection = (String) product.getSelectedItem();
        if (selection == null) {
            warning("Chọn thuốc trong danh mục.");
            return;
        }
        try {
            LocalDate date = LocalDate.parse(expiry.getText().trim());
            ChiTietPhieuNhap line = new ChiTietPhieuNhap(selection.split(" - ", 2)[0],
                    lot.getText(), date, Integer.parseInt(quantity.getText().trim()),
                    Long.parseLong(cost.getText().trim()));
            service.kiemTraDong(line, LocalDate.now());
            if (lines.stream().anyMatch(item -> item.getKhoaLo().equals(line.getKhoaLo()))) {
                warning("Lô đã có trên phiếu. Bỏ dòng cũ trước khi nhập lại.");
                return;
            }
            lines.add(line);
            for (JTextField field : new JTextField[]{lot, expiry, quantity, cost}) {
                field.setText("");
            }
            refreshDraft();
        } catch (DateTimeParseException error) {
            warning("Nhập hạn sử dụng theo dạng YYYY-MM-DD.");
        } catch (NumberFormatException error) {
            warning("Nhập số lượng và đơn giá bằng số nguyên hợp lệ.");
        } catch (IllegalArgumentException error) {
            warning(error.getMessage());
        }
    }

    private void save() {
        NhaCungCap selected = (NhaCungCap) supplier.getSelectedItem();
        if (receiptId.getText().trim().isEmpty() || lines.isEmpty() || selected == null
                || employee.getSelectedItem() == null) {
            warning("Nhập thông tin phiếu và thêm ít nhất một lô đạt kiểm tra.");
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Bạn đã đối chiếu chứng từ và kiểm tra tất cả số lượng thực nhận trên phiếu?",
                "Xác nhận nhập kho", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            PhieuNhap saved = service.xacNhanNhap(receiptId.getText(), selected.getMaNCC(),
                    (String) employee.getSelectedItem(), document.getText(), lines);
            lines.clear();
            receiptId.setText("");
            document.setText("");
            refreshDraft();
            refreshHistory();
            JOptionPane.showMessageDialog(this, "Đã ghi phiếu " + saved.getMaPN() + " và cập nhật tồn kho.");
        } catch (IllegalArgumentException error) {
            warning(error.getMessage());
        }
    }

    private void refreshDraft() {
        draftModel.setRowCount(0);
        lines.forEach(line -> addRow(draftModel, line));
        total.setText("Tổng tiền nhập " + money(totalOf(lines)));
    }

    private void refreshHistory() {
        historyTable.clearSelection();
        history.clear();
        history.addAll(service.getLichSu());
        historyModel.setRowCount(0);
        for (PhieuNhap receipt : history) {
            historyModel.addRow(new Object[]{receipt.getMaPN(), TIME.format(receipt.getNgayNhap()),
                    receipt.getMaNCC(), receipt.getMaNV(), receipt.getSoChungTu(),
                    money(totalOf(receipt.getChiTiet()))});
        }
        if (!history.isEmpty()) {
            historyTable.setRowSelectionInterval(0, 0);
        }
    }

    private void addRow(DefaultTableModel model, ChiTietPhieuNhap line) {
        Thuoc medicine = db.thuocs.get(line.getMaThuoc());
        model.addRow(new Object[]{line.getMaThuoc(), medicine.getTenThuoc(), medicine.getDonViTinh(),
                line.getMaLo(), line.getHanSuDung(), line.getSoLuongNhap(), money(line.getDonGiaNhap())});
    }

    private static BigInteger totalOf(List<ChiTietPhieuNhap> lines) {
        return lines.stream()
                .map(line -> BigInteger.valueOf(line.getDonGiaNhap()).multiply(BigInteger.valueOf(line.getSoLuongNhap())))
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    private static String money(Number amount) {
        return String.format(VI, "%,d đ", amount);
    }

    private void warning(String message) {
        JOptionPane.showMessageDialog(this, message, "Phiếu nhập hàng", JOptionPane.WARNING_MESSAGE);
    }

    private static JPanel paddedPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(new EmptyBorder(12, 14, 12, 14));
        return panel;
    }

    private static void addFields(JPanel panel, String[] labels, JComponent... fields) {
        for (String label : labels) {
            panel.add(new JLabel(label));
        }
        for (JComponent field : fields) {
            panel.add(field);
        }
    }

    private static DefaultTableModel lotModel() {
        return readOnly("Mã thuốc", "Tên thuốc", "Đơn vị", "Mã lô", "Hạn sử dụng", "SL nhập", "Đơn giá");
    }

    private static DefaultTableModel readOnly(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
    }
}
