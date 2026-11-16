package com.longchau.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PhieuNhap {
    private final String maPN;
    private final String maNCC;
    private final String maNV;
    private final String soChungTu;
    private final LocalDateTime ngayNhap;
    private final List<ChiTietPhieuNhap> chiTiet;

    public PhieuNhap(String maPN, String maNCC, String maNV, String soChungTu, LocalDateTime ngayNhap,
                    List<ChiTietPhieuNhap> chiTiet) {
        this.maPN = maPN;
        this.maNCC = maNCC;
        this.maNV = maNV;
        this.soChungTu = soChungTu;
        this.ngayNhap = ngayNhap;
        this.chiTiet = Collections.unmodifiableList(new ArrayList<>(chiTiet));
    }

    public String getMaPN() { return maPN; }
    public String getMaNCC() { return maNCC; }
    public String getMaNV() { return maNV; }
    public String getSoChungTu() { return soChungTu; }
    public LocalDateTime getNgayNhap() { return ngayNhap; }
    public List<ChiTietPhieuNhap> getChiTiet() { return chiTiet; }
}
