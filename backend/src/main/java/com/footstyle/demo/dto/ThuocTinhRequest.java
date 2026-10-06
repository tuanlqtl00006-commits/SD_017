package com.footstyle.demo.dto;

/** Thêm / sửa một thuộc tính (danh mục, thương hiệu, màu sắc, ...). Mã do hệ thống tự cấp và không đổi, chỉ gửi tên. */
public record ThuocTinhRequest(String ten) {
}
