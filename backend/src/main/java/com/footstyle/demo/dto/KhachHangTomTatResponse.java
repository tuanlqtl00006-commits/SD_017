package com.footstyle.demo.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Khách hàng trong phần chọn / danh sách khách được tặng phiếu cá nhân.
 * daDung: đã dùng phiếu hay chưa.
 * soDonDaMua: số hóa đơn của khách (không tính đơn đã hủy).
 * lanMuaGanNhat: thời điểm tạo hóa đơn gần nhất (null nếu chưa mua lần nào).
 */
public record KhachHangTomTatResponse(
        Integer id,
        String ma,
        String hoTen,
        String soDienThoai,
        String email,
        LocalDate ngaySinh,
        long soDonDaMua,
        LocalDateTime lanMuaGanNhat,
        boolean daDung) {
}
