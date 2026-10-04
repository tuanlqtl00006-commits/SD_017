package com.footstyle.demo.dto;

public record BienTheResponse(
        Integer id,
        String ma,
        Integer idSanPham,
        Integer idMauSac,
        Integer idTrongLuong,
        Integer idChuVi,
        Long giaBan,
        Integer soLuongTon,
        boolean hoatDong) {
}
