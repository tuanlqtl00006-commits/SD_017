package com.footstyle.demo.dto;

/** Đổi trạng thái hóa đơn. ghiChu bắt buộc khi hủy (lý do hủy). */
public record HoaDonTrangThaiRequest(Integer trangThaiMoi, String ghiChu, String nguoiThaoTac) {
}
