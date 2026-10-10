package com.footstyle.demo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

/**
 * Dữ liệu thêm / sửa đợt giảm giá (trang "Thêm đợt giảm giá" / "Chi tiết đợt giảm giá").
 *
 * <p>Ngày gửi dạng chuỗi, nhận được cả hai kiểu:
 * <ul>
 *   <li>"yyyy-MM-dd": ngày bắt đầu tính từ 00:00:00, ngày kết thúc tính đến hết ngày (23:59:59);</li>
 *   <li>"yyyy-MM-ddTHH:mm:ss": dùng đúng thời điểm gửi lên.</li>
 * </ul>
 *
 * <p>idBienThe (nhận cả tên cũ "idBienThes"): danh sách id biến thể (san_pham_chi_tiet) được áp dụng.
 * <ul>
 *   <li>Thêm mới: bắt buộc chọn ít nhất 1 biến thể.</li>
 *   <li>Sửa: null = giữ nguyên biến thể đang áp dụng; có danh sách = đồng bộ đúng theo danh sách
 *       (biến thể không còn trong danh sách sẽ được gỡ khỏi đợt, biến thể mới được thêm vào).</li>
 * </ul>
 */
public record DotGiamGiaRequest(
        String maDot,
        String tenDot,
        Integer phanTramGiamDot,
        String ngayBatDau,
        String ngayKetThuc,
        String moTa,
        Integer trangThai,
        @JsonAlias("idBienThes") List<Long> idBienThe) {
}
