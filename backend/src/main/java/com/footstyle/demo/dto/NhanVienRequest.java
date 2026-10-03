package com.footstyle.demo.dto;

import java.time.LocalDate;

/** Dữ liệu thêm / sửa nhân viên. matKhau bắt buộc khi thêm; khi sửa để trống nghĩa là giữ mật khẩu cũ. */
public record NhanVienRequest(
        String hoTen,
        String gioiTinh,
        LocalDate ngaySinh,
        String soDienThoai,
        String email,
        String diaChi,
        LocalDate ngayVaoLam,
        Integer idVaiTro,
        String matKhau) {
}
