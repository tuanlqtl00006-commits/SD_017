package com.footstyle.demo.dto;

import java.time.LocalDate;
import java.util.List;

/** Không bao giờ trả mật khẩu về frontend. diaChi = địa chỉ chính ghép thành một dòng (dùng cho danh sách). */
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
        boolean hoatDong,
        String cccd,
        List<ViTriResponse> viTri,
        List<DiaChiNhanVienDto> diaChis) {
}
