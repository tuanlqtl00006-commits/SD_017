package com.footstyle.demo.dto;

import java.util.List;

/** "nhom" chỉ có ở chất liệu (các nhóm danh mục dùng chất liệu này); các thuộc tính khác để null. */
public record ThuocTinhResponse(Integer id, String ma, String ten, boolean hoatDong, List<String> nhom) {
}
