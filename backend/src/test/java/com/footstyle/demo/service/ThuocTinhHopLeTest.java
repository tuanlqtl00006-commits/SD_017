package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Cô: giá trị thuộc tính phải đúng chuẩn thị trường, không nhập size / màu linh tinh. */
class ThuocTinhHopLeTest {

    private static String hl(String loai, String ten) {
        return ThuocTinhHopLe.loi(loai, ThuocTinhHopLe.chuanHoa(loai, ten));
    }

    @Test
    void mauSac_chiChu_khongSoKhongKyTuLa() {
        assertNull(hl("mau-sac", "Xanh dương"));
        assertNull(hl("mau-sac", "Trắng - Đỏ"));
        assertNull(hl("mau-sac", "Đen/Vàng"));
        assertNotNull(hl("mau-sac", "Hồng 45"));
        assertNotNull(hl("mau-sac", "Đỏ!!"));
        assertNotNull(hl("mau-sac", "123"));
    }

    @Test
    void trongLuong_chuanU_vaKhoangGramDung() {
        assertEquals("4U (80-84g)", ThuocTinhHopLe.chuanHoa("trong-luong", " 4u ( 80 - 84 g ) "));
        assertNull(hl("trong-luong", "4U (80-84g)"));
        assertNull(hl("trong-luong", "3U"));
        assertTrue(hl("trong-luong", "4U (85-89g)").contains("4U (80-84g)"), "khoảng gram phải đúng chuẩn của 4U");
        assertNotNull(hl("trong-luong", "9U"));
        assertNotNull(hl("trong-luong", "nặng"));
        assertNull(hl("trong-luong", "Không áp dụng"));
    }

    @Test
    void chuVi_G1denG6() {
        assertEquals("G5", ThuocTinhHopLe.chuanHoa("chu-vi", "g5"));
        assertNull(hl("chu-vi", "G4"));
        assertNotNull(hl("chu-vi", "G7"));
        assertNotNull(hl("chu-vi", "5"));
        assertNotNull(hl("chu-vi", "XL"));
    }

    @Test
    void cacBangKhac() {
        assertNull(hl("do-cung", "Siêu cứng"));
        assertNotNull(hl("do-cung", "Cứng 2"));
        assertNull(hl("xuat-xu", "Đài Loan"));
        assertNotNull(hl("xuat-xu", "VN-01"));
        assertNull(hl("thuong-hieu", "Li-Ning"));
        assertNull(hl("chat-lieu", "Carbon 30T"));
        assertNull(hl("chat-lieu", "Polyester 100%"));
        assertNotNull(hl("thuong-hieu", "12345"));
        assertNotNull(hl("danh-muc", "Vợt @@"));
        assertNull(hl("danh-muc", "Túi - balo"));
    }
}
