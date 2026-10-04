package com.footstyle.demo.dto;

import java.time.LocalDate;

/**
 * Dữ liệu thêm / sửa nhân viên.
 * - Thêm mới: dùng tất cả các trường.
 * - Sửa: chỉ dùng hoTen, gioiTinh, ngaySinh, soDienThoai, diaChi, idVaiTro.
 *   email, ngayVaoLam, matKhau không được sửa nên nếu có gửi lên cũng bị bỏ qua.
 */
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
