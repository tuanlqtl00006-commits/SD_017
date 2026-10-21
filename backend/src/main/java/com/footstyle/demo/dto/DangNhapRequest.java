package com.footstyle.demo.dto;

/** Đăng nhập: taiKhoan = email, số điện thoại hoặc mã nhân viên. */
public record DangNhapRequest(String taiKhoan, String matKhau) {
}
