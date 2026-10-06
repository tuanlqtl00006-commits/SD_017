package com.footstyle.demo.dto;

import java.util.List;





public record SanPhamRequest(
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
        List<String> anhPhu) {
}
