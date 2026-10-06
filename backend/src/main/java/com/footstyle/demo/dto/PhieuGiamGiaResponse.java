package com.footstyle.demo.dto;

import java.time.LocalDate;
import java.util.List;

public record PhieuGiamGiaResponse(
        Integer id,
        String ma,
        String ten,
        String hinhThuc,
        String loaiGiam,
        Long giaTri,
        Long giamToiDa,
        Long donToiThieu,
        Integer soLuong,
        Integer soLuongDaDung,
        Integer gioiHanMoiKhach,
        LocalDate ngayBatDau,
        LocalDate ngayKetThuc,
        boolean hoatDong,
        List<KhachHangTomTatResponse> khachHangs) {
}
