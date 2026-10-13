package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Chất liệu theo danh mục: nhóm suy ra từ tên danh mục, chất liệu chỉ dùng cho danh mục thuộc nhóm của nó. */
class NhomDanhMucTest {

    @Test
    void nhanRaNhomTuTenDanhMuc() {
        assertEquals("VOT", NhomDanhMuc.tuTen("Vợt cầu lông"));
        assertEquals("VOT", NhomDanhMuc.tuTen("Vợt cầu lông người mới"));
        assertEquals("QUAN_AO", NhomDanhMuc.tuTen("Quần áo thể thao"));
        assertEquals("TUI_BALO", NhomDanhMuc.tuTen("Túi - balo"));
        assertEquals("PHU_KIEN", NhomDanhMuc.tuTen("Phụ kiện"));
        assertEquals("QUA_CAU", NhomDanhMuc.tuTen("Quả cầu lông"));
        assertNull(NhomDanhMuc.tuTen("Giày"));
        assertNull(NhomDanhMuc.tuTen(null));
        assertNull(NhomDanhMuc.tuTen("  "));
    }

    @Test
    void quanAoKhongDungChatLieuKhungVot() {
        assertFalse(NhomDanhMuc.hop(NhomDanhMuc.tach("VOT"), "Quần áo thể thao")); // Nhôm / Titanium / Carbon
        assertTrue(NhomDanhMuc.hop(NhomDanhMuc.tach("QUAN_AO,PHU_KIEN"), "Quần áo thể thao")); // Cotton
        assertTrue(NhomDanhMuc.hop(NhomDanhMuc.tach("VOT"), "Vợt cầu lông"));
    }

    @Test
    void danhMucLaHoacChatLieuChuaGanNhomThiKhongBiLoc() {
        assertTrue(NhomDanhMuc.hop(NhomDanhMuc.tach("VOT"), "Giày"));
        assertTrue(NhomDanhMuc.hop(NhomDanhMuc.tach(null), "Quần áo thể thao"));
        assertTrue(NhomDanhMuc.hop(NhomDanhMuc.tach(""), "Quần áo thể thao"));
    }

    @Test
    void tachChuoiNhom() {
        assertEquals(List.of("VOT", "QUAN_AO"), NhomDanhMuc.tach("VOT, QUAN_AO,,VOT"));
        assertEquals(List.of(), NhomDanhMuc.tach(null));
    }
}
