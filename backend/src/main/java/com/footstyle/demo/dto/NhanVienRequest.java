package com.footstyle.demo.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Dữ liệu thêm / sửa nhân viên.
 * - Thêm mới: dùng tất cả các trường. Mã nhân viên do hệ thống tạo theo họ tên đầy đủ, không nhận từ request.
 * - Sửa: dùng hoTen, gioiTinh, ngaySinh, soDienThoai, cccd, diaChis, idVaiTro, idViTri.
 *   email, ngayVaoLam, matKhau không được sửa nên nếu có gửi lên cũng bị bỏ qua.
 * - cccd (số căn cước, 12 số) và idViTri (các vị trí làm việc, chọn nhiều) không bắt buộc, dùng cho cả thêm và sửa.
 * - diaChis: danh sách địa chỉ (id = null là địa chỉ mới; địa chỉ không có trong danh sách vẫn được giữ, không bị xóa).
 *   diaChi (chuỗi) chỉ còn để tương thích bản cũ: dùng khi không gửi diaChis.
 */
public record NhanVienRequest(
        String hoTen,
        String gioiTinh,
        LocalDate ngaySinh,
        String soDienThoai,
        String email,
        String diaChi,
        LocalDate ngayVaoLam,
        Integer idVaiTro,
        String matKhau,
        String cccd,
        List<Integer> idViTri,
        List<DiaChiNhanVienDto> diaChis) {
}
