package com.footstyle.demo.dto;

/** Thêm / sửa biến thể (san_pham_chi_tiet). Khi sửa: không đổi mã và không đổi sản phẩm. */
public record BienTheRequest(
        Integer idSanPham,
        Integer idMauSac,
        Integer idTrongLuong,
        Integer idChuVi,
        String ma,
        Long giaBan,
        Integer soLuongTon) {
}
