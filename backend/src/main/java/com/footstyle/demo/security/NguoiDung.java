package com.footstyle.demo.security;

/**
 * Người đang gọi API (lấy từ token đăng nhập).
 * loai: NV = nhân viên / quản lý, KH = khách hàng. vaiTro: QUAN_LY | NHAN_VIEN | KHACH_HANG.
 */
public record NguoiDung(String loai, Integer id, String ma, String hoTen, String vaiTro, String email) {

    public static final String LOAI_NHAN_VIEN = "NV";
    public static final String LOAI_KHACH_HANG = "KH";

    public static final String QUAN_LY = "QUAN_LY";
    public static final String NHAN_VIEN = "NHAN_VIEN";
    public static final String KHACH_HANG = "KHACH_HANG";

    public boolean laQuanLy() {
        return QUAN_LY.equals(vaiTro);
    }

    /** Nhân viên hoặc quản lý (người làm việc trong cửa hàng). */
    public boolean laNhanVien() {
        return LOAI_NHAN_VIEN.equals(loai);
    }

    public boolean laKhachHang() {
        return LOAI_KHACH_HANG.equals(loai);
    }

    /** Tên vai trò hiển thị. */
    public String tenVaiTro() {
        if (laQuanLy()) return "Quản lý";
        if (laKhachHang()) return "Khách hàng";
        return "Nhân viên";
    }
}
