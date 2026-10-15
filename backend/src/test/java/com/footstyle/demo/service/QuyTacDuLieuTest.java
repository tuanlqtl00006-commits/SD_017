package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Bộ luật dữ liệu dùng chung cho Khách hàng và Nhân viên (thêm, sửa, nhập Excel). */
class QuyTacDuLieuTest {

    private static final LocalDate HOM_NAY = LocalDate.of(2026, 10, 15);

    // ---------- số điện thoại ----------
    @Test
    void sdtChiNhanDauSo03_05_07_08_09Va10ChuSo() {
        for (String ok : new String[] {"0912345678", "0351234567", "0501234567", "0781234567", "0891234567", "0987654321", "0399999999"}) {
            assertTrue(QuyTacDuLieu.SDT.matcher(ok).matches(), ok);
        }
        for (String sai : new String[] {"0212345678", "0412345678", "0612345678", "0112345678", "0012345678", "091234567", "09123456789", "+84912345678",
                "84912345678", "0912 345 678", "0912-345-678", "09123abc78", "", " 0912345678", "0912345678 ", "912345678"}) {
            assertFalse(QuyTacDuLieu.SDT.matcher(sai).matches(), "[" + sai + "]");
        }
    }

    // ---------- email ----------
    @Test
    void emailPhaiCoDangTenAtMienDauChamVaKhongKhoangTrang() {
        for (String ok : new String[] {"an@gmail.com", "an.nv@footstyle.vn", "a+b@x.co", "an_nv@sub.domain.vn", "A@B.CD"}) {
            assertTrue(QuyTacDuLieu.EMAIL.matcher(ok).matches(), ok);
        }
        for (String sai : new String[] {"", "an", "an@", "@gmail.com", "an@gmail", "an@gmail.", "an@gmail.c", "an @gmail.com", "an@gm ail.com", "a@@b.com"}) {
            assertFalse(QuyTacDuLieu.EMAIL.matcher(sai).matches(), "[" + sai + "]");
        }
    }

    // ---------- họ tên ----------
    @Test
    void hoTenChiGomChuCaiCoDauKhoangTrangVaMotSoDauCau() {
        for (String ok : new String[] {"Nguyễn Văn An", "Trần Thị Mai Lan", "Đỗ Hải", "Ngô Bảo Châu", "O'Connor", "Trần Thị-Mai", "Lê  Văn", "Nguyễn A."}) {
            assertTrue(QuyTacDuLieu.HO_TEN.matcher(ok).matches(), ok);
        }
        for (String sai : new String[] {"", "123", "An 2", "An1", "@An", "An_B", "An#", " An", "-An", "An/Bình", "An\tBình", "An<script>"}) {
            assertFalse(QuyTacDuLieu.HO_TEN.matcher(sai).matches(), "[" + sai + "]");
        }
    }

    @Test
    void chuanHoaHoTenBoKhoangTrangThua() {
        assertEquals("Nguyễn Văn An", QuyTacDuLieu.chuanHoaHoTen("  Nguyễn   Văn \t An  "));
        assertEquals("", QuyTacDuLieu.chuanHoaHoTen(null));
        assertEquals("", QuyTacDuLieu.chuanHoaHoTen("   "));
        assertEquals("", QuyTacDuLieu.cat(null));
        assertEquals("x", QuyTacDuLieu.cat("  x "));
    }

    // ---------- căn cước ----------
    @Test
    void canCuocDung12ChuSo() {
        assertTrue(QuyTacDuLieu.CCCD.matcher("001099012345").matches());
        assertFalse(QuyTacDuLieu.CCCD.matcher("00109901234").matches());
        assertFalse(QuyTacDuLieu.CCCD.matcher("0010990123456").matches());
        assertFalse(QuyTacDuLieu.CCCD.matcher("00109901234a").matches());
    }

    // ---------- tuổi ----------
    @Test
    void khachHangTu15Tuoi_dungNgaySinhNhatThu15LaDuoc() {
        assertNull(QuyTacDuLieu.kiemTraNgaySinh(HOM_NAY.minusYears(15), QuyTacDuLieu.TUOI_KHACH_TOI_THIEU, "Khách hàng", HOM_NAY), "đúng sinh nhật 15 tuổi");
        assertEquals("Khách hàng phải từ đủ 15 tuổi.",
                QuyTacDuLieu.kiemTraNgaySinh(HOM_NAY.minusYears(15).plusDays(1), QuyTacDuLieu.TUOI_KHACH_TOI_THIEU, "Khách hàng", HOM_NAY), "còn 1 ngày nữa mới đủ 15");
        assertEquals("Khách hàng phải từ đủ 15 tuổi.",
                QuyTacDuLieu.kiemTraNgaySinh(HOM_NAY.minusYears(10), QuyTacDuLieu.TUOI_KHACH_TOI_THIEU, "Khách hàng", HOM_NAY));
        assertNull(QuyTacDuLieu.kiemTraNgaySinh(HOM_NAY.minusYears(15).minusDays(1), QuyTacDuLieu.TUOI_KHACH_TOI_THIEU, "Khách hàng", HOM_NAY));
    }

    @Test
    void nhanVienTu18Tuoi() {
        assertNull(QuyTacDuLieu.kiemTraNgaySinh(HOM_NAY.minusYears(18), QuyTacDuLieu.TUOI_NHAN_VIEN_TOI_THIEU, "Nhân viên", HOM_NAY));
        assertEquals("Nhân viên phải từ đủ 18 tuổi.",
                QuyTacDuLieu.kiemTraNgaySinh(HOM_NAY.minusYears(18).plusDays(1), QuyTacDuLieu.TUOI_NHAN_VIEN_TOI_THIEU, "Nhân viên", HOM_NAY));
        assertEquals("Nhân viên phải từ đủ 18 tuổi.",
                QuyTacDuLieu.kiemTraNgaySinh(HOM_NAY.minusYears(16), QuyTacDuLieu.TUOI_NHAN_VIEN_TOI_THIEU, "Nhân viên", HOM_NAY));
    }

    @Test
    void ngaySinhTuongLaiVaQua100Tuoi() {
        assertEquals("Ngày sinh không được ở tương lai.", QuyTacDuLieu.kiemTraNgaySinh(HOM_NAY.plusDays(1), 15, "Khách hàng", HOM_NAY));
        assertNull(QuyTacDuLieu.kiemTraNgaySinh(HOM_NAY.minusYears(100), 15, "Khách hàng", HOM_NAY), "đúng 100 tuổi vẫn được");
        assertEquals("Ngày sinh không hợp lệ (quá 100 tuổi).", QuyTacDuLieu.kiemTraNgaySinh(HOM_NAY.minusYears(101), 15, "Khách hàng", HOM_NAY));
        assertEquals("Ngày sinh không hợp lệ (quá 100 tuổi).", QuyTacDuLieu.kiemTraNgaySinh(LocalDate.of(1900, 1, 1), 15, "Khách hàng", HOM_NAY));
        assertNull(QuyTacDuLieu.kiemTraNgaySinh(null, 15, "Khách hàng", HOM_NAY), "không nhập ngày sinh: việc bắt buộc hay không do form quyết định");
    }

    @Test
    void mocTuoiLaDungVoiLuatDaGhiTrongTaiLieu() {
        assertEquals(15, QuyTacDuLieu.TUOI_KHACH_TOI_THIEU, "Bộ luật Dân sự 2015, Điều 21 khoản 4");
        assertEquals(18, QuyTacDuLieu.TUOI_NHAN_VIEN_TOI_THIEU, "Bộ luật Lao động 2019, Điều 143");
        assertEquals(100, QuyTacDuLieu.TUOI_TOI_DA);
    }

    @Test
    void tuoiTronTheoNgay() {
        assertEquals(26, QuyTacDuLieu.tuoi(LocalDate.of(2000, 10, 15), HOM_NAY));
        assertEquals(25, QuyTacDuLieu.tuoi(LocalDate.of(2000, 10, 16), HOM_NAY));
    }

    @Test
    void kiemTraAnhDaiDien() {
        assertNull(QuyTacDuLieu.kiemTraAnhDaiDien(null));
        assertNull(QuyTacDuLieu.kiemTraAnhDaiDien("   "));
        assertNull(QuyTacDuLieu.kiemTraAnhDaiDien("/api/uploads/3f2a-9c_1.JPG"));
        assertNull(QuyTacDuLieu.kiemTraAnhDaiDien("HTTP://abc.vn/a.png"));
        assertNull(QuyTacDuLieu.kiemTraAnhDaiDien("https://abc.vn/a.png?x=1"));
        for (String sai : new String[] {"javascript:alert(1)", "/api/uploads/", "/api/uploads/../a", "/uploads/a.png", "a.png"}) {
            assertNotNull(QuyTacDuLieu.kiemTraAnhDaiDien(sai), sai);
        }
        assertEquals("Đường dẫn ảnh đại diện tối đa 500 ký tự.", QuyTacDuLieu.kiemTraAnhDaiDien("https://a.vn/" + "x".repeat(490)));
    }
}
