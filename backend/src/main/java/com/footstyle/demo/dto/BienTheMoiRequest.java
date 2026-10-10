package com.footstyle.demo.dto;

/** Một biến thể gửi kèm khi thêm sản phẩm (mã biến thể do hệ thống tự sinh: <mã SP>-<MÀU>-<TRỌNG LƯỢNG>-<CHU VI>). */
public record BienTheMoiRequest(
        Integer idMauSac,
        Integer idTrongLuong,
        Integer idChuVi,
        Long giaBan,
        Integer soLuongTon) {
}
