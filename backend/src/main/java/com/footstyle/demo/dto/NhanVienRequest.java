package com.footstyle.demo.dto;

import java.time.LocalDate;







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
