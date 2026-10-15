package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.mail.Message;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Mail thông báo tài khoản gửi cho nhân viên mới (nhập Excel): thư ghép đúng, đếm theo lô, email hỏng không làm hỏng cả lô. */
class MailTaiKhoanNhanVienTest {

    private static Map<String, Object> doiXong(MailService s, String lo, int tong) throws Exception {
        Map<String, Object> kq = s.ketQuaLo(lo);
        for (int i = 0; i < 200 && !(Boolean.TRUE.equals(kq.get("xong")) && ((Integer) kq.get("tong")) == tong); i++) {
            Thread.sleep(25);
            kq = s.ketQuaLo(lo);
        }
        return kq;
    }

    @Test
    void thuCoHtmlBanChuVaLogo_guiDungNguoiNhan() throws Exception {
        MailServiceTest.GhiThu sender = new MailServiceTest.GhiThu();
        MailService s = new MailService(new MailServiceTest.NoiCap(sender), "", "shop@gmail.com", "mk", "Cửa Hàng Cầu Lông", "https://footstyle.vn");
        s.batDauLo("NVIMP-1", 1);
        s.guiTaiKhoanNhanVien("Nguyễn Văn An", "  an.nv@footstyle.vn ", "NVA01", "Nhân viên", "Ab3dEf7hJk", "NVIMP-1");
        Map<String, Object> kq = doiXong(s, "NVIMP-1", 1);
        assertEquals(1, kq.get("daGui"));

        MimeMessage m = MailServiceTest.docLai(sender.daGui.get(0));
        assertEquals("an.nv@footstyle.vn", ((InternetAddress) m.getRecipients(Message.RecipientType.TO)[0]).getAddress(), "bỏ khoảng trắng thừa");
        assertEquals("[Cửa Hàng Cầu Lông] Tài khoản nhân viên của bạn đã được tạo", m.getSubject());
        InternetAddress tu = (InternetAddress) m.getFrom()[0];
        assertEquals("shop@gmail.com", tu.getAddress());
        assertEquals("Cửa Hàng Cầu Lông", tu.getPersonal());

        List<Part> phan = MailServiceTest.cacPhanLa(m);
        String chu = (String) MailServiceTest.phanLoai(phan, "text/plain").getContent();
        String html = (String) MailServiceTest.phanLoai(phan, "text/html").getContent();
        for (String t : new String[] {chu, html}) {
            assertTrue(t.contains("NVA01") && t.contains("Ab3dEf7hJk") && t.contains("an.nv@footstyle.vn"), t);
        }
        assertTrue(html.contains("<a href=\"https://footstyle.vn\""));
        assertNotNull(MailServiceTest.phanLoai(phan, "image/jpeg"), "kèm logo");
    }

    @Test
    void emailHongThiChiLoNayBaoLoi_cacThuKhacVanGui() throws Exception {
        MailServiceTest.GhiThu sender = new MailServiceTest.GhiThu();
        MailService s = new MailService(new MailServiceTest.NoiCap(sender), "", "shop@gmail.com", "mk", "FootStyle", "");
        s.batDauLo("NVIMP-2", 3);
        s.guiTaiKhoanNhanVien("An", "an@footstyle.vn", "A01", "Nhân viên", "Ab3dEf7hJk", "NVIMP-2");
        s.guiTaiKhoanNhanVien("Bình", "khong-co-email", "B01", "Nhân viên", "Cd4eFg8hJk", "NVIMP-2");
        s.guiTaiKhoanNhanVien("Cúc", null, "C01", "Nhân viên", "Ef5gHj9kLm", "NVIMP-2");
        Map<String, Object> kq = doiXong(s, "NVIMP-2", 3);
        assertEquals(3, kq.get("tong"));
        assertEquals(1, kq.get("daGui"));
        assertEquals(2, kq.get("loi"));
        assertEquals(true, kq.get("xong"));
        assertTrue(kq.get("lyDo").toString().contains("C01"), "lý do nêu mã nhân viên không có email: " + kq.get("lyDo"));
        assertEquals(1, sender.daGui.size());
    }

    @Test
    void khongDatTruocSoThuThiTuDemTheoSoThuXep() throws Exception {
        MailServiceTest.GhiThu sender = new MailServiceTest.GhiThu();
        MailService s = new MailService(new MailServiceTest.NoiCap(sender), "", "shop@gmail.com", "mk", "FootStyle", "");
        s.guiTaiKhoanNhanVien("An", "an@footstyle.vn", "A01", "Nhân viên", "Ab3dEf7hJk", "NVIMP-3");
        s.guiTaiKhoanNhanVien("Bình", "binh@footstyle.vn", "B01", "Quản lý", "Cd4eFg8hJk", "NVIMP-3");
        Map<String, Object> kq = doiXong(s, "NVIMP-3", 2);
        assertEquals(2, kq.get("tong"));
        assertEquals(2, kq.get("daGui"));
    }

    @Test
    void maLoKhongPhanBietHoaThuong() throws Exception {
        MailServiceTest.GhiThu sender = new MailServiceTest.GhiThu();
        MailService s = new MailService(new MailServiceTest.NoiCap(sender), "", "shop@gmail.com", "mk", "FootStyle", "");
        s.batDauLo("nvimp-4", 1);
        s.guiTaiKhoanNhanVien("An", "an@footstyle.vn", "A01", "Nhân viên", "Ab3dEf7hJk", "NVIMP-4");
        assertEquals(1, doiXong(s, "NVIMP-4", 1).get("daGui"));
    }

    @Test
    void loGuiLoi_baoLyDoThatCuaGmail() throws Exception {
        MailServiceTest.GhiThu sender = new MailServiceTest.GhiThu();
        sender.loi = true;
        MailService s = new MailService(new MailServiceTest.NoiCap(sender), "", "shop@gmail.com", "mk", "FootStyle", "");
        s.batDauLo("NVIMP-5", 1);
        s.guiTaiKhoanNhanVien("An", "an@footstyle.vn", "A01", "Nhân viên", "Ab3dEf7hJk", "NVIMP-5");
        Map<String, Object> kq = doiXong(s, "NVIMP-5", 1);
        assertEquals(1, kq.get("loi"));
        assertTrue(kq.get("lyDo").toString().contains("Mật khẩu ứng dụng"), "có gợi ý cách sửa: " + kq.get("lyDo"));
    }
}
