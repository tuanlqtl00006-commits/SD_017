package com.footstyle.demo.dto;

import java.time.LocalDate;

/** Khách hàng tự cập nhật hồ sơ ở trang "Tài khoản" của cửa hàng online. */
public record KhachHoSoRequest(String hoTen, String sdt, String email, LocalDate ngaySinh, Integer gioiTinh) {
}
