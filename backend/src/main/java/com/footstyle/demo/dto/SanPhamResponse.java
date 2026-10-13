package com.footstyle.demo.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * anhChinh / anhPhu: ảnh chung của sản phẩm. anhTheoMau: ảnh riêng từng màu.
 * anhDaiDien: ảnh để hiện ở danh sách = ảnh chính chung, không có thì lấy ảnh chính của màu đầu tiên (có thể rỗng).
 */
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
        boolean hoatDong,
        List<AnhTheoMauResponse> anhTheoMau,
        String anhDaiDien) {
}
