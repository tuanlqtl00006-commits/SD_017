package com.footstyle.demo.dto;

import java.util.List;

/**
 * Thêm / sửa một thuộc tính (danh mục, thương hiệu, màu sắc, ...). Mã do hệ thống tự cấp và không đổi, chỉ gửi tên.
 * Riêng chất liệu có thêm "nhom": các nhóm danh mục dùng chất liệu này (VOT, QUAN_AO, TUI_BALO, PHU_KIEN, QUA_CAU).
 */
public record ThuocTinhRequest(String ten, List<String> nhom) {
}
