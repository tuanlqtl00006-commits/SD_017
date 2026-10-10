package com.footstyle.demo.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * ketQuaMail: chỉ có khi vừa thêm / sửa phiếu cá nhân, ví dụ "Đã gửi mail: 2 phiếu mới, 1 cập nhật, 1 hủy." (không thì null).
 */
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
        List<KhachHangTomTatResponse> khachHangs,
        String ketQuaMail) {

    /** Bản sao có kèm thông báo mail. */
    public PhieuGiamGiaResponse voiKetQuaMail(String thongBao) {
        return new PhieuGiamGiaResponse(id, ma, ten, hinhThuc, loaiGiam, giaTri, giamToiDa, donToiThieu, soLuong,
                soLuongDaDung, gioiHanMoiKhach, ngayBatDau, ngayKetThuc, hoatDong, khachHangs, thongBao);
    }
}
