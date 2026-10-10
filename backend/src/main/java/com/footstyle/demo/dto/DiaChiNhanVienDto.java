package com.footstyle.demo.dto;

/**
 * Một địa chỉ của nhân viên (dùng cho cả gửi lên và trả về).
 * id = null: địa chỉ mới thêm. macDinh = true: địa chỉ chính (đúng 1 địa chỉ chính).
 */
public record DiaChiNhanVienDto(
        Integer id,
        String tinhThanh,
        String phuongXa,
        String diaChiCuThe,
        boolean macDinh) {
}
