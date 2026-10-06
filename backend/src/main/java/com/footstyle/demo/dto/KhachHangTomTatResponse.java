package com.footstyle.demo.dto;


public record KhachHangTomTatResponse(
        Long id,
        String ma,
        String hoTen,
        String soDienThoai,
        String email,
        boolean daDung) {
}
