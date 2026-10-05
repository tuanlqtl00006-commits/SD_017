package com.footstyle.demo.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Dữ liệu tạo / sửa phiếu giảm giá.
 * hinhThuc: CONG_KHAI (doi_tuong_ap_dung = 1) | CA_NHAN (doi_tuong_ap_dung = 2)
 * loaiGiam: PHAN_TRAM (loai_giam = 1) | TIEN_MAT (loai_giam = 2)
 */
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
