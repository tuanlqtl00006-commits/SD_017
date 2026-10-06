package com.footstyle.demo.dto;

/** Thêm / sửa biến thể (san_pham_chi_tiet). Khi sửa: được đổi sản phẩm; trường ma bị bỏ qua (mã không sửa tay, chỉ phần đầu mã đổi theo sản phẩm mới). */
public record BienTheRequest(
        Integer idSanPham,
        Integer idMauSac,
        Integer idTrongLuong,
        Integer idChuVi,
        String ma,
        Long giaBan,
        Integer soLuongTon) {
}
