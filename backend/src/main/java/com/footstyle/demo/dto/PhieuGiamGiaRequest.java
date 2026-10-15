package com.footstyle.demo.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Dữ liệu tạo / sửa phiếu giảm giá.
 * hinhThuc: CONG_KHAI (doi_tuong_ap_dung = 1) | CA_NHAN (doi_tuong_ap_dung = 2)
 * loaiGiam: PHAN_TRAM (loai_giam = 1) | TIEN_MAT (loai_giam = 2)
 *
 * ngayBatDau / ngayKetThuc là NGÀY GIỜ ("yyyy-MM-ddTHH:mm" hoặc "yyyy-MM-ddTHH:mm:ss"), tính theo phút:
 * phiếu có hiệu lực từ đầu phút bắt đầu đến hết phút kết thúc (kết thúc 17:00 nghĩa là dùng được tới 17:00:59).
 * soLuong chỉ dùng cho phiếu CÔNG KHAI; phiếu cá nhân bỏ qua, số lượng luôn bằng số khách được chọn.
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
        LocalDateTime ngayBatDau,
        LocalDateTime ngayKetThuc,
        List<Integer> khachHangIds) {
}
