package com.footstyle.demo.dto;

import java.time.LocalDate;
import java.util.List;






public record PhieuGiamGiaRequest(
        String ma,
        String ten,
        String hinhThuc,
        String loaiGiam,
        Long giaTri,
        Long giamToiDa,
        Long donToiThieu,
        Integer soLuong,
        Integer gioiHanMoiKhach,
        LocalDate ngayBatDau,
        LocalDate ngayKetThuc,
        List<Long> khachHangIds) {}
