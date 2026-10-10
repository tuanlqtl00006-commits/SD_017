package com.footstyle.demo.dto;

import java.util.List;

/**
 * Dữ liệu thêm / sửa sản phẩm. ma chỉ dùng khi thêm (sửa thì giữ mã cũ).
 * anhChinh + anhPhu được lưu vào bảng hinh_anh_san_pham (anhChinh có la_anh_chinh = 1).
 *
 * Chỉ khi THÊM: bienThes = các biến thể (san_pham_chi_tiet) lưu cùng sản phẩm trong một lần.
 * Nếu sản phẩm đã tồn tại (cùng tên + danh mục + thương hiệu) thì backend trả 409 mã SAN_PHAM_DA_TON_TAI;
 * frontend hỏi "bạn muốn cập nhật?" rồi gửi lại với xacNhanCapNhat = true để cập nhật sản phẩm cũ
 * (thêm biến thể mới, biến thể trùng thì cập nhật giá bán / số lượng tồn).
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
        List<String> anhPhu,
        List<BienTheMoiRequest> bienThes,
        Boolean xacNhanCapNhat) {
}
