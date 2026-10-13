package com.footstyle.demo.dto;

import java.util.List;

/**
 * Ảnh của một màu khi thêm / sửa sản phẩm: anh[0] là ảnh chính của màu, các ảnh sau là ảnh phụ.
 * Ảnh này áp dụng cho MỌI biến thể cùng màu (vd "Ảnh sản phẩm màu Đen" dùng cho Đen 3U G5, Đen 4U G6...).
 */
public record AnhTheoMauRequest(Integer idMauSac, List<String> anh) {
}
