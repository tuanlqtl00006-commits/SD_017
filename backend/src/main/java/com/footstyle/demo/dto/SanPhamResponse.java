package com.footstyle.demo.dto;

import java.time.LocalDate;
import java.util.List;

public record SanPhamResponse(
        Integer id,
        String ma,
        String ten,
        Integer idDanhMuc,
        Integer idThuongHieu,
        Integer idXuatXu,
        Integer idChatLieu,
        Integer idDoCung,
        Integer idDiemCanBang,
        String moTa,
        String anhChinh,
        List<String> anhPhu,
        LocalDate ngayTao,
        LocalDate ngayCapNhat,
        boolean hoatDong) {
}
