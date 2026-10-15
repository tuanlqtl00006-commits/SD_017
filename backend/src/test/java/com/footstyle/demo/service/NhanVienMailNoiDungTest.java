package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.service.PhieuMailNoiDung.ThuMail;
import org.junit.jupiter.api.Test;

/** Nội dung mail thông báo tài khoản cho nhân viên mới (nhập Excel): tiêu đề, bản chữ, bản HTML, chống chèn mã HTML. */
class NhanVienMailNoiDungTest {

    private final NhanVienMailNoiDung noiDung = new NhanVienMailNoiDung("FootStyle", "https://footstyle.vn/dang-nhap");

    @Test
    void tieuDeCoTenCuaHang_trongThiDungFootStyle() {
        assertEquals("[FootStyle] Tài khoản nhân viên của bạn đã được tạo", noiDung.tieuDe());
        assertEquals("[Cầu Lông Hà Nội] Tài khoản nhân viên của bạn đã được tạo", new NhanVienMailNoiDung("  Cầu Lông Hà Nội ", null).tieuDe());
        assertEquals("[FootStyle] Tài khoản nhân viên của bạn đã được tạo", new NhanVienMailNoiDung("  ", null).tieuDe());
        assertEquals("[FootStyle] Tài khoản nhân viên của bạn đã được tạo", new NhanVienMailNoiDung(null, null).tieuDe());
    }

    @Test
    void banChuCoDuThongTinDangNhapVaLoiNhacDoiMatKhau() {
        String chu = noiDung.banChu("Nguyễn Văn An", "an.nv@footstyle.vn", "NVA01", "Nhân viên", "Ab3dEf7hJk");
        assertTrue(chu.startsWith("Xin chào Nguyễn Văn An,"));
        assertTrue(chu.contains("- Mã nhân viên: NVA01"));
        assertTrue(chu.contains("- Vai trò: Nhân viên"));
        assertTrue(chu.contains("- Tài khoản (email): an.nv@footstyle.vn"));
        assertTrue(chu.contains("- Mật khẩu tạm: Ab3dEf7hJk"));
        assertTrue(chu.contains("- Địa chỉ đăng nhập: https://footstyle.vn/dang-nhap"));
        assertTrue(chu.contains("đổi mật khẩu ngay sau lần đăng nhập đầu tiên"));
    }

    @Test
    void khongCoLinkHopLeThiBoDongDiaChiDangNhap() {
        for (String link : new String[] {null, "", "  ", "footstyle.vn", "javascript:alert(1)", "ftp://x.vn", "https://"}) {
            NhanVienMailNoiDung n = new NhanVienMailNoiDung("FootStyle", link);
            String chu = n.banChu("An", "an@x.vn", "A01", "Nhân viên", "Ab3dEf7hJk");
            assertFalse(chu.contains("Địa chỉ đăng nhập"), "link: " + link);
            assertFalse(n.html("An", "an@x.vn", "A01", "Nhân viên", "Ab3dEf7hJk", false).contains("<a href"), "link: " + link);
        }
    }

    @Test
    void htmlDungBoCucBangVaCoDuThongTin() {
        String h = noiDung.html("Nguyễn Văn An", "an.nv@footstyle.vn", "NVA01", "Quản lý", "Ab3dEf7hJk", true);
        assertTrue(h.startsWith("<!DOCTYPE html>"));
        assertTrue(h.contains("<meta charset=\"UTF-8\">"));
        assertTrue(h.contains("cid:logo"), "có logo kiểu CID");
        assertTrue(h.contains("NVA01") && h.contains("Quản lý") && h.contains("an.nv@footstyle.vn") && h.contains("Ab3dEf7hJk"));
        assertTrue(h.contains("<a href=\"https://footstyle.vn/dang-nhap\""));
        assertTrue(h.contains("Lưu ý bảo mật"));
        assertTrue(h.contains("max-width:600px"));
        assertFalse(h.contains("<script"));
    }

    @Test
    void khongLogoThiKhongThamChieuCid() {
        String h = noiDung.html("An", "an@x.vn", "A01", "Nhân viên", "Ab3dEf7hJk", false);
        assertFalse(h.contains("cid:"));
        assertTrue(h.contains("FootStyle"));
    }

    @Test
    void tenVaMaChuaKyTuHtmlDuocThoatDeKhongChenDuocMa() {
        String h = noiDung.html("<script>alert(1)</script> & \"An\"", "a<b>@x.vn", "A\"01", "Nhân <b>viên</b>", "x<y>&z", false);
        assertFalse(h.contains("<script>alert(1)</script>"));
        assertTrue(h.contains("&lt;script&gt;alert(1)&lt;/script&gt; &amp; &quot;An&quot;"));
        assertFalse(h.contains("a<b>@x.vn"));
        assertTrue(h.contains("a&lt;b&gt;@x.vn"));
        assertTrue(h.contains("x&lt;y&gt;&amp;z"));
    }

    @Test
    void thieuTenThiXungHoLaBan() {
        assertTrue(noiDung.banChu(null, "an@x.vn", "A01", "Nhân viên", "Ab3dEf7hJk").startsWith("Xin chào bạn,"));
        assertTrue(noiDung.banChu("  ", "an@x.vn", "A01", "Nhân viên", "Ab3dEf7hJk").startsWith("Xin chào bạn,"));
    }

    @Test
    void soanTraDuBaPhan() {
        ThuMail thu = noiDung.soan("An", "an@x.vn", "A01", "Nhân viên", "Ab3dEf7hJk", true);
        assertEquals(noiDung.tieuDe(), thu.tieuDe());
        assertEquals(noiDung.banChu("An", "an@x.vn", "A01", "Nhân viên", "Ab3dEf7hJk"), thu.banChu());
        assertEquals(noiDung.html("An", "an@x.vn", "A01", "Nhân viên", "Ab3dEf7hJk", true), thu.html());
    }
}
