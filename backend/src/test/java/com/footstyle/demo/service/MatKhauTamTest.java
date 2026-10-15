package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Mật khẩu tạm gửi cho nhân viên mới: ngẫu nhiên, đạt luật mật khẩu của nhân viên, không có ký tự dễ nhìn nhầm. */
class MatKhauTamTest {

    @Test
    void luonDuoc10KyTuCoHoaThuongSoVaDatLuatMatKhauNhanVien() {
        for (int i = 0; i < 2000; i++) {
            String mk = MatKhauTam.tao();
            assertEquals(10, mk.length());
            assertTrue(mk.matches(".*[A-Z].*"), mk);
            assertTrue(mk.matches(".*[a-z].*"), mk);
            assertTrue(mk.matches(".*\\d.*"), mk);
            assertTrue(mk.matches("[A-Za-z0-9]+"), "chỉ chữ và số, không khoảng trắng: " + mk);
            assertTrue(mk.length() >= 8 && mk.length() <= 50, "đạt luật 8-50 ký tự của form nhân viên");
        }
    }

    @Test
    void khongCoKyTuDeNhinNham() {
        for (int i = 0; i < 2000; i++) {
            String mk = MatKhauTam.tao();
            assertTrue(mk.chars().noneMatch(c -> "0O1lIo".indexOf(c) >= 0), mk);
        }
    }

    @Test
    void ngauNhienKhongLapLai() {
        Set<String> ds = new HashSet<>();
        for (int i = 0; i < 1000; i++) ds.add(MatKhauTam.tao());
        assertEquals(1000, ds.size());
    }

    @Test
    void cacKyTuBatBuocKhongLuonDungDauChuoi() {
        int hoaDau = 0;
        for (int i = 0; i < 1000; i++) {
            if (Character.isUpperCase(MatKhauTam.tao().charAt(0))) hoaDau++;
        }
        assertTrue(hoaDau < 700, "đã xáo trộn nên chữ hoa không luôn đứng đầu: " + hoaDau);
    }
}
