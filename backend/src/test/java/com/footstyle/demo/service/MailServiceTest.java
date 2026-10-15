package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.entity.PhieuGiamGia;
import com.footstyle.demo.service.PhieuMailNoiDung.Loai;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Kiểm tra MailService với MimeMessage thật của Jakarta Mail (không cần SMTP):
 * thư ghép đúng (HTML + bản chữ + logo CID), gửi nền, lỗi gửi chỉ ghi log, chưa có SMTP thì không gửi.
 */
class MailServiceTest {

    /** Bộ gửi giả: ghi lại thư thay vì gửi thật; bật loi thì mỗi lần gửi đều báo lỗi. */
    static class GhiThu extends JavaMailSenderImpl {
        final List<MimeMessage> daGui = new CopyOnWriteArrayList<>();
        volatile boolean loi;
        final AtomicInteger soLanGoi = new AtomicInteger();

        @Override
        protected void doSend(MimeMessage[] mimeMessages, Object[] originalMessages) {
            soLanGoi.incrementAndGet();
            if (loi) {
                throw new IllegalStateException("535 sai mật khẩu");
            }
            for (MimeMessage m : mimeMessages) daGui.add(m);
        }
    }

    /** Nơi cấp bộ gửi: chỉ cần 4 phương thức bắt buộc của ObjectProvider. */
    static class NoiCap implements ObjectProvider<JavaMailSender> {
        final JavaMailSender sender;
        final AtomicInteger soLanHoi = new AtomicInteger();

        NoiCap(JavaMailSender sender) {
            this.sender = sender;
        }

        @Override
        public JavaMailSender getObject(Object... args) {
            return sender;
        }

        @Override
        public JavaMailSender getIfAvailable() {
            soLanHoi.incrementAndGet();
            return sender;
        }

        @Override
        public JavaMailSender getIfUnique() {
            return sender;
        }

        @Override
        public JavaMailSender getObject() {
            return sender;
        }
    }

    private static PhieuGiamGia phieu() {
        PhieuGiamGia p = new PhieuGiamGia();
        p.setMaPhieu("VCH2ABC");
        p.setTenPhieu("Tri ân khách hàng");
        p.setLoaiGiam(PhieuGiamGia.LOAI_PHAN_TRAM);
        p.setGiaTriGiam(10L);
        p.setGiamToiDa(200_000L);
        p.setGiaTriDonHangToiThieu(500_000L);
        p.setNgayBatDau(LocalDateTime.of(2026, 10, 9, 0, 0));
        p.setNgayKetThuc(LocalDateTime.of(2026, 10, 15, 23, 59, 59));
        return p;
    }

    private static KhachHang khach(String hoTen, String email) {
        KhachHang k = new KhachHang();
        k.setId(2);
        k.setHoTen(hoTen);
        k.setEmail(email);
        return k;
    }

    private static MailService dich(NoiCap noiCap) {
        return new MailService(noiCap, "", "shop@gmail.com", "matkhau-ung-dung", "Cửa Hàng Cầu Lông", "https://footstyle.vn");
    }

    /** Ghi thư ra dạng chữ thô rồi đọc lại như máy nhận thư. */
    static MimeMessage docLai(MimeMessage m) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        m.writeTo(out);
        return new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(out.toByteArray()));
    }

    static void thuThapPhan(Part p, List<Part> ra) throws Exception {
        if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) p.getContent();
            for (int i = 0; i < mp.getCount(); i++) thuThapPhan(mp.getBodyPart(i), ra);
        } else {
            ra.add(p);
        }
    }

    static List<Part> cacPhanLa(MimeMessage m) throws Exception {
        List<Part> ra = new ArrayList<>();
        thuThapPhan(m, ra);
        return ra;
    }

    static Part phanLoai(List<Part> ds, String kieu) throws Exception {
        for (Part p : ds) if (p.isMimeType(kieu)) return p;
        return null;
    }

    static void doiDuThu(GhiThu g, int soThu) throws Exception {
        for (int i = 0; i < 200 && g.daGui.size() < soThu; i++) Thread.sleep(25);
    }

    /* ---------- ghép thư ---------- */

    @Test
    void thuCoHtmlBanChuVaLogoCid() throws Exception {
        GhiThu sender = new GhiThu();
        MailService s = dich(new NoiCap(sender));
        assertNotNull(s.logo, "tìm thấy static/logo.jpg trong resources");

        var thu = new PhieuMailNoiDung("Cửa Hàng Cầu Lông", "https://footstyle.vn")
                .soan(Loai.MOI, phieu(), khach("Nguyễn Văn An", "an@mail.vn"), true);
        MimeMessage m = docLai(s.soanThu(sender, "an@mail.vn", thu, s.logo));

        // Người gửi / người nhận / tiêu đề (có dấu tiếng Việt)
        InternetAddress tu = (InternetAddress) m.getFrom()[0];
        assertEquals("shop@gmail.com", tu.getAddress());
        assertEquals("Cửa Hàng Cầu Lông", tu.getPersonal());
        Address[] den = m.getRecipients(Message.RecipientType.TO);
        assertEquals(1, den.length);
        assertEquals("an@mail.vn", ((InternetAddress) den[0]).getAddress());
        assertEquals(thu.tieuDe(), m.getSubject());
        assertTrue(m.getSubject().contains("Bạn nhận được phiếu giảm giá VCH2ABC"));

        // Thân thư: bản chữ + bản HTML + logo
        List<Part> phan = cacPhanLa(m);
        Part chu = phanLoai(phan, "text/plain");
        Part html = phanLoai(phan, "text/html");
        Part anh = phanLoai(phan, "image/jpeg");
        assertNotNull(chu, "có bản chữ");
        assertNotNull(html, "có bản HTML");
        assertNotNull(anh, "có logo");
        assertEquals(thu.banChu(), ((String) chu.getContent()).replace("\r\n", "\n"));
        assertEquals(thu.html(), ((String) html.getContent()).replace("\r\n", "\n"));
        assertTrue(((String) html.getContent()).contains("cid:logo"));
        assertTrue(html.getContentType().toLowerCase().contains("utf-8"), html.getContentType());
        assertTrue(chu.getContentType().toLowerCase().contains("utf-8"), chu.getContentType());

        // Logo gắn kiểu CID, đúng nội dung ảnh
        assertEquals("<logo>", ((jakarta.mail.internet.MimeBodyPart) anh).getContentID());
        assertEquals(Part.INLINE, anh.getDisposition());
        try (InputStream in = anh.getInputStream()) {
            assertArrayEquals(s.logo, in.readAllBytes());
        }
        // Cấu trúc: thư chính là multipart, chữ nằm trước HTML (máy đọc chọn bản cuối cùng hiểu được)
        assertTrue(m.isMimeType("multipart/mixed"));
        assertTrue(phan.indexOf(chu) < phan.indexOf(html));
    }

    @Test
    void khongCoLogoThiThuKhongCoAnhVaHtmlKhongThamChieuCid() throws Exception {
        GhiThu sender = new GhiThu();
        MailService s = dich(new NoiCap(sender));
        var thu = new PhieuMailNoiDung("Cửa Hàng Cầu Lông", "").soan(Loai.CAP_NHAT, phieu(), khach("An", "an@mail.vn"), false);
        MimeMessage m = docLai(s.soanThu(sender, "an@mail.vn", thu, null));
        List<Part> phan = cacPhanLa(m);
        assertNull(phanLoai(phan, "image/*"));
        assertFalse(((String) phanLoai(phan, "text/html").getContent()).contains("cid:"));
        assertNotNull(phanLoai(phan, "text/plain"));
    }

    @Test
    void tenNguoiGuiCoDauVaTenCuaHangKhongPhaThu() throws Exception {
        GhiThu sender = new GhiThu();
        MailService s = new MailService(new NoiCap(sender), "no-reply@footstyle.vn", "shop@gmail.com", "matkhau-ung-dung", "Đại Lý Vợt Cầu Lông", "");
        var thu = new PhieuMailNoiDung("Đại Lý Vợt Cầu Lông", "").soan(Loai.HUY, phieu(), khach("An", "an@mail.vn"), true);
        MimeMessage m = docLai(s.soanThu(sender, "an@mail.vn", thu, s.logo));
        InternetAddress tu = (InternetAddress) m.getFrom()[0];
        assertEquals("no-reply@footstyle.vn", tu.getAddress(), "app.mail.from được ưu tiên hơn spring.mail.username");
        assertEquals("Đại Lý Vợt Cầu Lông", tu.getPersonal());
    }

    @Test
    void thuHuyKhongChuaMucGiamKhiXemTrongThuThat() throws Exception {
        GhiThu sender = new GhiThu();
        MailService s = dich(new NoiCap(sender));
        var thu = new PhieuMailNoiDung("Cửa Hàng Cầu Lông", "https://footstyle.vn").soan(Loai.HUY, phieu(), khach("An", "an@mail.vn"), true);
        List<Part> phan = cacPhanLa(docLai(s.soanThu(sender, "an@mail.vn", thu, s.logo)));
        String html = (String) phanLoai(phan, "text/html").getContent();
        String chu = (String) phanLoai(phan, "text/plain").getContent();
        for (String t : new String[] {html, chu}) {
            assertTrue(t.contains("VCH2ABC"));
            assertFalse(t.contains("10%"));
            assertFalse(t.contains("500.000"));
            assertFalse(t.contains("Hiệu lực"));
        }
    }

    /* ---------- gửi nền ---------- */

    @Test
    void guiPhieuGiamGia_xepVaoHangDoiRoiGuiThuThat() throws Exception {
        GhiThu sender = new GhiThu();
        MailService s = dich(new NoiCap(sender));
        s.guiPhieuGiamGia(Loai.MOI, phieu(), khach("Nguyễn Văn An", "  an@mail.vn  "));
        doiDuThu(sender, 1);
        assertEquals(1, sender.daGui.size());
        MimeMessage m = docLai(sender.daGui.get(0));
        assertEquals("an@mail.vn", ((InternetAddress) m.getRecipients(Message.RecipientType.TO)[0]).getAddress(), "bỏ khoảng trắng thừa");
        List<Part> phan = cacPhanLa(m);
        String html = (String) phanLoai(phan, "text/html").getContent();
        assertTrue(html.contains("Nguyễn Văn An"));
        assertTrue(html.contains("VCH2ABC"));
        assertTrue(html.contains("<a href=\"https://footstyle.vn\""), "có nút khi cấu hình app.mail.link-cua-hang");
        assertNotNull(phanLoai(phan, "image/jpeg"));
    }

    @Test
    void guiPhieuGiamGia_baLoaiMailDeuGuiDuocVaDungTieuDe() throws Exception {
        GhiThu sender = new GhiThu();
        MailService s = dich(new NoiCap(sender));
        for (Loai loai : Loai.values()) {
            s.guiPhieuGiamGia(loai, phieu(), khach("An", "an@mail.vn"));
        }
        doiDuThu(sender, 3);
        assertEquals(3, sender.daGui.size());
        List<String> tieuDe = new ArrayList<>();
        for (MimeMessage m : sender.daGui) tieuDe.add(m.getSubject());
        assertTrue(tieuDe.stream().anyMatch(t -> t.contains("Bạn nhận được phiếu giảm giá")));
        assertTrue(tieuDe.stream().anyMatch(t -> t.contains("đã được cập nhật")));
        assertTrue(tieuDe.stream().anyMatch(t -> t.contains("đã bị hủy")));
    }

    @Test
    void khachKhongCoEmailHoacEmailSaiThiKhongGui() throws Exception {
        GhiThu sender = new GhiThu();
        NoiCap noiCap = new NoiCap(sender);
        MailService s = dich(noiCap);
        s.guiPhieuGiamGia(Loai.MOI, phieu(), khach("A", null));
        s.guiPhieuGiamGia(Loai.MOI, phieu(), khach("B", "khong-co-a-cong"));
        s.guiPhieuGiamGia(Loai.MOI, phieu(), null);
        Thread.sleep(300);
        assertEquals(0, sender.soLanGoi.get());
        assertEquals(0, noiCap.soLanHoi.get());
        assertFalse(MailService.coEmail(khach("A", null)));
        assertFalse(MailService.coEmail(null));
        assertTrue(MailService.coEmail(khach("A", "a@b.vn")));
    }

    @Test
    void chuaCauHinhSmtp_khongGuiVaKhongLoi() throws Exception {
        NoiCap khongCoSmtp = new NoiCap(null);
        MailService s = dich(khongCoSmtp);
        s.guiPhieuGiamGia(Loai.MOI, phieu(), khach("An", "an@mail.vn"));
        s.guiPhieuGiamGia(Loai.HUY, phieu(), khach("An", "an@mail.vn"));
        for (int i = 0; i < 100 && khongCoSmtp.soLanHoi.get() < 2; i++) Thread.sleep(25);
        assertEquals(2, khongCoSmtp.soLanHoi.get(), "đã đi qua nhánh ghi log giả lập, không ném lỗi ra ngoài");
    }

    /* ---------- đã cấu hình Gmail chưa (nguyên nhân thường gặp khiến "không thấy gửi mail") ---------- */

    @Test
    void duocCauHinh_chiKhiCoBoGuiVaDuEmailLanMatKhauUngDung() {
        GhiThu sender = new GhiThu();
        NoiCap co = new NoiCap(sender);
        assertTrue(new MailService(co, "", "shop@gmail.com", "mat-khau", "FootStyle", "").duocCauHinh());
        assertFalse(new MailService(new NoiCap(null), "", "shop@gmail.com", "mat-khau", "FootStyle", "").duocCauHinh(), "chưa có spring.mail.host");
        assertFalse(new MailService(co, "", "", "mat-khau", "FootStyle", "").duocCauHinh(), "chưa điền email gửi");
        assertFalse(new MailService(co, "", "shop@gmail.com", "", "FootStyle", "").duocCauHinh(), "chưa điền mật khẩu ứng dụng");
        assertFalse(new MailService(co, "", "shop@gmail.com", "   ", "FootStyle", "").duocCauHinh(), "mật khẩu toàn khoảng trắng");
        assertFalse(new MailService(co, "", null, null, "FootStyle", "").duocCauHinh());
    }

    @Test
    void coHostNhungChuaDienEmailMatKhau_khongGuiThatVaKhongLoi() throws Exception {
        // File mail.properties mẫu đã có host smtp.gmail.com nhưng email / mật khẩu còn trống: không được thử đăng nhập Gmail
        GhiThu sender = new GhiThu();
        MailService s = new MailService(new NoiCap(sender), "", "", "", "FootStyle", "");
        s.guiPhieuGiamGia(Loai.MOI, phieu(), khach("An", "an@mail.vn"));
        Thread.sleep(300);
        assertEquals(0, sender.soLanGoi.get(), "chưa điền tài khoản thì không gửi");
        assertEquals(0, sender.daGui.size());
    }

    @Test
    void guiThuThu_chuaCauHinhThiHuongDanDienFileMailProperties() {
        MailService s = new MailService(new NoiCap(null), "", "", "", "FootStyle", "");
        String kq = s.guiThuThu();
        assertTrue(kq.startsWith("CHƯA cấu hình Gmail"), kq);
        assertTrue(kq.contains("mail.properties"));
    }

    @Test
    void guiThuThu_daCauHinhThiGuiNgayDenChinhEmailGui() throws Exception {
        GhiThu sender = new GhiThu();
        MailService s = dich(new NoiCap(sender));
        String kq = s.guiThuThu();
        assertTrue(kq.startsWith("Đã gửi thư thử đến shop@gmail.com"), kq);
        assertEquals(1, sender.daGui.size(), "gửi ngay, không qua hàng đợi nền");
        MimeMessage m = docLai(sender.daGui.get(0));
        assertEquals("shop@gmail.com", ((InternetAddress) m.getRecipients(Message.RecipientType.TO)[0]).getAddress());
        assertEquals("shop@gmail.com", ((InternetAddress) m.getFrom()[0]).getAddress());
        assertTrue(m.getSubject().contains("Thư thử"));
    }

    @Test
    void guiThuThu_guiLoiThiTraVeLoiThatKemGoiYMatKhauUngDung() {
        GhiThu sender = new GhiThu();
        sender.loi = true; // ném IllegalStateException("535 sai mật khẩu")
        MailService s = dich(new NoiCap(sender));
        String kq = s.guiThuThu();
        assertTrue(kq.startsWith("Gửi thư thử THẤT BẠI: 535 sai mật khẩu"), kq);
        assertTrue(kq.contains("Mật khẩu ứng dụng"), "gợi ý dùng mật khẩu ứng dụng khi gặp lỗi 535");
    }

    @Test
    void loiThat_layCauLoiGocVaGopYTheoLoai() {
        Exception boc = new RuntimeException("Mail server connection failed", new IllegalStateException("535-5.7.8 Username and Password not accepted"));
        assertEquals("535-5.7.8 Username and Password not accepted", MailService.loiThat(boc));
        assertTrue(MailService.gopY(boc).contains("apppasswords"));
        Exception mang = new RuntimeException("x", new java.net.UnknownHostException("smtp.gmail.com"));
        assertTrue(MailService.gopY(mang).contains("587"));
        assertEquals("", MailService.gopY(new RuntimeException("lỗi lạ")));
        assertEquals("NullPointerException", MailService.loiThat(new NullPointerException()));
    }

    @Test
    void lamSachMatKhau_boKhoangTrangThuaVaGhiVaoBoGuiSmtp() {
        // Google hiện mật khẩu ứng dụng dạng "abcd efgh ijkl mnop"; dán nguyên vào file (kèm dấu cách / xuống dòng) vẫn phải đăng nhập được
        assertEquals("abcdefghijklmnop", MailService.lamSachMatKhau(" abcd efgh\tijkl mnop \n"));
        assertEquals("", MailService.lamSachMatKhau(null));
        GhiThu sender = new GhiThu();
        MailService s = new MailService(new NoiCap(sender), "", " shop@gmail.com ", "abcd efgh ijkl mnop ", "FootStyle", "");
        s.chuanHoaTaiKhoan();
        assertEquals("shop@gmail.com", sender.getUsername());
        assertEquals("abcdefghijklmnop", sender.getPassword());
    }

    @Test
    void gopY_loiChungChiSslDoPhanMemDietVirus() {
        Exception e = new RuntimeException("x", new javax.net.ssl.SSLHandshakeException(
                "PKIX path building failed: unable to find valid certification path to requested target"));
        assertTrue(MailService.gopY(e).contains("diệt virus"));
    }

    @Test
    void guiLoiChiGhiLog_thuSauVanGuiDuoc() throws Exception {
        GhiThu sender = new GhiThu();
        MailService s = dich(new NoiCap(sender));
        sender.loi = true;
        s.guiPhieuGiamGia(Loai.MOI, phieu(), khach("An", "an@mail.vn"));
        for (int i = 0; i < 100 && sender.soLanGoi.get() < 1; i++) Thread.sleep(25);
        assertEquals(1, sender.soLanGoi.get());
        assertEquals(0, sender.daGui.size());

        sender.loi = false;
        s.guiPhieuGiamGia(Loai.MOI, phieu(), khach("Bình", "binh@mail.vn"));
        doiDuThu(sender, 1);
        assertEquals(1, sender.daGui.size(), "lỗi lần trước không làm hỏng hàng đợi");
    }

    @Test
    void emailKhongDungDinhDang_loiGhepThuChiGhiLog() throws Exception {
        GhiThu sender = new GhiThu();
        MailService s = dich(new NoiCap(sender));
        // Có "@" nên qua được coEmail nhưng không phải địa chỉ hợp lệ -> soanThu báo lỗi, được bắt trong gui()
        s.guiPhieuGiamGia(Loai.MOI, phieu(), khach("An", "an khong hop le@mail.vn"));
        Thread.sleep(300);
        assertEquals(0, sender.daGui.size());
        s.guiPhieuGiamGia(Loai.MOI, phieu(), khach("Bình", "binh@mail.vn"));
        doiDuThu(sender, 1);
        assertEquals(1, sender.daGui.size());
    }

    @Test
    void khongCoLogo_thuVanGuiBinhThuong() throws Exception {
        GhiThu sender = new GhiThu();
        MailService s = dich(new NoiCap(sender));
        s.logo = null; // như khi không tìm thấy file logo
        s.guiPhieuGiamGia(Loai.MOI, phieu(), khach("An", "an@mail.vn"));
        doiDuThu(sender, 1);
        assertEquals(1, sender.daGui.size());
        List<Part> phan = cacPhanLa(docLai(sender.daGui.get(0)));
        assertNull(phanLoai(phan, "image/*"));
        assertFalse(((String) phanLoai(phan, "text/html").getContent()).contains("cid:"));
    }

    /* ---------- logo ---------- */

    @Test
    void docLogo_coFileThiTraVeAnhJpeg() {
        byte[] b = MailService.docLogo(new ClassPathResource("static/logo.jpg"));
        assertNotNull(b);
        assertTrue(b.length > 1000);
        assertEquals((byte) 0xFF, b[0]);
        assertEquals((byte) 0xD8, b[1]);
    }

    @Test
    void docLogo_khongCoFileHoacDocLoiThiTraVeNull() {
        assertNull(MailService.docLogo(new ClassPathResource("static/khong-co-file.jpg")));
        Resource hong = new ByteArrayResource(new byte[0]) {
            @Override
            public InputStream getInputStream() throws IOException {
                throw new IOException("đĩa lỗi");
            }
        };
        assertNull(MailService.docLogo(hong));
    }
}
