package com.footstyle.demo.dto;

/** hinhAnh: ảnh chính của MÀU của biến thể; màu chưa có ảnh thì lấy ảnh chính chung của sản phẩm (có thể rỗng). */
public record BienTheResponse(
        Integer id,
        String ma,
        Integer idSanPham,
        Integer idMauSac,
        Integer idTrongLuong,
        Integer idChuVi,
        Long giaBan,
        Integer soLuongTon,
        boolean hoatDong,
        String hinhAnh) {
}
