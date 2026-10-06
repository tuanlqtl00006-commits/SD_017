package com.footstyle.demo.dto;


public record BienTheRequest(
        Integer idSanPham,
        Integer idMauSac,
        Integer idTrongLuong,
        Integer idChuVi,
        String ma,
        Long giaBan,
        Integer soLuongTon) {
}
