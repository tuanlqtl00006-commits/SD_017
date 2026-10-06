package com.footstyle.demo.dto;

import java.util.List;

/**
 * Dữ liệu thêm / sửa sản phẩm. ma chỉ dùng khi thêm (sửa thì giữ mã cũ).
 * anhChinh + anhPhu được lưu vào bảng hinh_anh_san_pham (anhChinh có la_anh_chinh = 1).
 */
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
