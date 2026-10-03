package com.footstyle.demo.dto;

import java.time.LocalDate;

/** Không bao giờ trả mật khẩu về frontend. */
public record NhanVienResponse(
        Integer id,
        String ma,
        String hoTen,
        String gioiTinh,
        LocalDate ngaySinh,
        String soDienThoai,
        String email,
        String diaChi,
        LocalDate ngayVaoLam,
        Integer idVaiTro,
        String vaiTro,
        boolean hoatDong) {
}
