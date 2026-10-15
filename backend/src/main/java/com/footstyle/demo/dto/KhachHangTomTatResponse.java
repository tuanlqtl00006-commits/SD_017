package com.footstyle.demo.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Khách hàng trong bảng chọn khi tặng phiếu cá nhân.
 * daDung: đã dùng phiếu hay chưa. gioiTinh: 1 = Nam, 0 = Nữ (null = chưa khai báo).
 * tongDon: số đơn ĐÃ HOÀN THÀNH của khách; muaGanNhat: ngày tạo đơn hoàn thành gần nhất (null = chưa mua).
 */
public record KhachHangTomTatResponse(
        Integer id,
        String ma,
        String hoTen,
        String soDienThoai,
        String email,
        boolean daDung,
        LocalDate ngaySinh,
        Integer gioiTinh,
        int tongDon,
        LocalDateTime muaGanNhat) {
}
