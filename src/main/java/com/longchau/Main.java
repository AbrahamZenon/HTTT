package com.longchau;

import com.longchau.view.PaymentManagementFrame;

import javax.swing.*;

/**
 * Lớp khởi động ứng dụng Java Swing - Nhà thuốc Long Châu
 * Module TV2: UC02 - Thanh toán hóa đơn & Quản lý trạng thái
 */
public class Main {
    public static void main(String[] args) {
        // Thiết lập giao diện Nimbus hoặc System Look and Feel cho đẹp
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored2) {}
        }

        // Chạy Frame trong Event Dispatch Thread của Swing
        SwingUtilities.invokeLater(() -> {
            PaymentManagementFrame frame = new PaymentManagementFrame();
            frame.setVisible(true);
        });
    }
}
