package com.longchau.model;

public final class NhaCungCap {
    private final String maNCC;
    private final String tenNCC;

    public NhaCungCap(String maNCC, String tenNCC) {
        this.maNCC = maNCC;
        this.tenNCC = tenNCC;
    }

    public String getMaNCC() { return maNCC; }
    public String getTenNCC() { return tenNCC; }

    @Override
    public String toString() { return maNCC + " - " + tenNCC; }
}
