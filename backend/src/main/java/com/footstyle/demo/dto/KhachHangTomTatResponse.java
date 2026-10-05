package com.footstyle.demo.dto;

/** Khách hàng được tặng phiếu cá nhân (daDung: đã dùng phiếu hay chưa). */
public record KhachHangTomTatResponse(
        Long id,
        Integer id,
        String ma,
        String hoTen,
        String soDienThoai,
        String email,
        boolean daDung) {
}
