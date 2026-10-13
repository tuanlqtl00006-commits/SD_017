package com.footstyle.demo.dto;

import java.util.List;

/** Ảnh của một màu: anh[0] là ảnh chính của màu (xem AnhTheoMauRequest). */
public record AnhTheoMauResponse(Integer idMauSac, List<String> anh) {
}
