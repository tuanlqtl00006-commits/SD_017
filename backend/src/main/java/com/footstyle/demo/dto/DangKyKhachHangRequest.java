package com.footstyle.demo.dto;

/** Khách tự đăng ký tài khoản ở cửa hàng online. Email không bắt buộc. */
public record DangKyKhachHangRequest(String hoTen, String sdt, String email, String matKhau, String xacNhanMatKhau) {
}
