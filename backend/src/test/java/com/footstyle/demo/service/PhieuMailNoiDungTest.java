package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.entity.PhieuGiamGia;
import com.footstyle.demo.service.PhieuMailNoiDung.Loai;
import com.footstyle.demo.service.PhieuMailNoiDung.ThuMail;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Kiểm tra phần dựng tiêu đề, bản chữ và bản HTML của mail phiếu giảm giá (không cần Spring chạy). */
class PhieuMailNoiDungTest {

    private final PhieuMailNoiDung noiDung = new PhieuMailNoiDung("FootStyle", "");

    // Ví dụ của cô: giảm 10%, đơn tối thiểu 500.000, tối đa 200.000, 09/10 -> 15/10
    private static PhieuGiamGia phieu() {
        PhieuGiamGia p = new PhieuGiamGia();
        p.setMaPhieu("VCH2ABC");
        p.setTenPhieu("Tri ân khách hàng thân thiết");
        p.setLoaiGiam(PhieuGiamGia.LOAI_PHAN_TRAM);
        p.setGiaTriGiam(10L);
        p.setGiamToiDa(200_000L);
        p.setGiaTriDonHangToiThieu(500_000L);
        p.setNgayBatDau(LocalDateTime.of(2026, 10, 9, 0, 0, 0));
        p.setNgayKetThuc(LocalDateTime.of(2026, 10, 15, 23, 59, 59));
        return p;
    }

    private static KhachHang khach(String hoTen) {
        KhachHang k = new KhachHang();
        k.setId(2);
        k.setHoTen(hoTen);
        k.setEmail("kh2@mail.vn");
        return k;
    }

    private String html(Loai loai) {
        return noiDung.html(loai, phieu(), khach("Nguyễn Văn An"), true);
    }

    /* ---------- tiêu đề ---------- */

    @Test
    void moiLoaiMailCoTieuDeDung() {
        assertEquals("[FootStyle] Bạn nhận được phiếu giảm giá VCH2ABC", noiDung.tieuDe(Loai.MOI, phieu()));
        assertEquals("[FootStyle] Phiếu giảm giá VCH2ABC đã được cập nhật", noiDung.tieuDe(Loai.CAP_NHAT, phieu()));
        assertEquals("[FootStyle] Phiếu giảm giá VCH2ABC đã bị hủy", noiDung.tieuDe(Loai.HUY, phieu()));
    }

    @Test
    void tenCuaHangTrongTieuDeLayTuCauHinh_trongThiDungFootStyle() {
        assertEquals("[Cầu Lông Hà Nội] Bạn nhận được phiếu giảm giá VCH2ABC",
                new PhieuMailNoiDung("Cầu Lông Hà Nội", null).tieuDe(Loai.MOI, phieu()));
        assertEquals("[FootStyle] Bạn nhận được phiếu giảm giá VCH2ABC",
                new PhieuMailNoiDung("  ", null).tieuDe(Loai.MOI, phieu()));
    }

    @Test
    void soanTraVeDuBaPhan() {
        ThuMail t = noiDung.soan(Loai.MOI, phieu(), khach("An"), true);
        assertEquals(noiDung.tieuDe(Loai.MOI, phieu()), t.tieuDe());
        assertEquals(noiDung.banChu(Loai.MOI, phieu(), khach("An")), t.banChu());
        assertEquals(noiDung.html(Loai.MOI, phieu(), khach("An"), true), t.html());
    }

    /* ---------- HTML: phần đầu và màu nhấn từng loại ---------- */

    @Test
    void htmlMoi_dauThuVaMauXanh() {
        String h = html(Loai.MOI);
        assertTrue(h.contains("Bạn nhận được phiếu giảm giá"));
        assertTrue(h.contains("#16a34a"), "màu nhấn xanh");
        assertFalse(h.contains("#ea580c"));
        assertFalse(h.contains("#dc2626"));
    }

    @Test
    void htmlCapNhat_dauThuVaMauCam() {
        String h = html(Loai.CAP_NHAT);
        assertTrue(h.contains("Phiếu giảm giá đã được cập nhật"));
        assertTrue(h.contains("thay cho thông tin cũ"));
        assertTrue(h.contains("#ea580c"), "màu nhấn cam");
        assertFalse(h.contains("#16a34a"));
        assertFalse(h.contains("#dc2626"));
    }

    @Test
    void htmlHuy_dauThuVaMauDo() {
        String h = html(Loai.HUY);
        assertTrue(h.contains("Phiếu giảm giá đã bị hủy"));
        assertTrue(h.contains("không còn sử dụng được"));
        assertTrue(h.contains("#dc2626"), "màu nhấn đỏ");
        assertFalse(h.contains("#16a34a"));
        assertFalse(h.contains("#ea580c"));
    }

    /* ---------- HTML: thẻ phiếu ---------- */

    @Test
    void htmlMoiVaCapNhat_coDayDuThongTinPhieu() {
        for (Loai loai : new Loai[] {Loai.MOI, Loai.CAP_NHAT}) {
            String h = html(loai);
            assertTrue(h.contains("VCH2ABC"), "mã phiếu " + loai);
            assertTrue(h.contains("Tri ân khách hàng thân thiết"), "tên phiếu " + loai);
            assertTrue(h.contains("Giảm 10%"), "mức giảm " + loai);
            assertTrue(h.contains("Giảm tối đa"), "nhãn giảm tối đa " + loai);
            assertTrue(h.contains("200.000 ₫"), "giảm tối đa " + loai);
            assertTrue(h.contains("Đơn tối thiểu"), "nhãn đơn tối thiểu " + loai);
            assertTrue(h.contains("500.000 ₫"), "đơn tối thiểu " + loai);
            assertTrue(h.contains("00:00 09/10/2026 – 23:59 15/10/2026"), "hiệu lực có cả giờ " + loai);
            assertTrue(h.contains("Nguyễn Văn An"), "lời chào " + loai);
        }
    }

    @Test
    void htmlHuy_khongHienMucGiamDonToiThieuHieuLuc() {
        String h = html(Loai.HUY);
        assertTrue(h.contains("VCH2ABC"));
        assertTrue(h.contains("Tri ân khách hàng thân thiết"));
        assertFalse(h.contains("10%"), "không hiện mức giảm");
        assertFalse(h.contains("Giảm"), "không có chữ Giảm nào (cả dòng xem trước)");
        assertFalse(h.contains("Mức giảm"));
        assertFalse(h.contains("200.000"));
        assertFalse(h.contains("Đơn tối thiểu"));
        assertFalse(h.contains("500.000"));
        assertFalse(h.contains("Hiệu lực"));
        assertFalse(h.contains("09/10/2026"));
        assertFalse(h.contains("15/10/2026"));
        assertFalse(h.contains("Mua sắm ngay"));
    }

    @Test
    void phieuTienMat_hienGiamSoTienVaKhongCoDongToiDa() {
        PhieuGiamGia p = phieu();
        p.setLoaiGiam(PhieuGiamGia.LOAI_SO_TIEN);
        p.setGiaTriGiam(50_000L);
        p.setGiamToiDa(null);
        String h = noiDung.html(Loai.MOI, p, khach("An"), false);
        assertTrue(h.contains("Giảm 50.000 ₫"));
        assertFalse(h.contains("Giảm tối đa"));
        assertEquals("giảm 50.000 ₫", PhieuMailNoiDung.mucGiam(p));
    }

    @Test
    void phieuPhanTramKhongGioiHanToiDa_khongCoDongToiDa() {
        PhieuGiamGia p = phieu();
        p.setGiamToiDa(null);
        String h = noiDung.html(Loai.MOI, p, khach("An"), false);
        assertTrue(h.contains("Giảm 10%"));
        assertFalse(h.contains("Giảm tối đa"));
        assertEquals("giảm 10%", PhieuMailNoiDung.mucGiam(p));
    }

    @Test
    void donToiThieuBangKhongHoacTrong_ghiKhongYeuCau() {
        for (Long v : new Long[] {0L, null}) {
            PhieuGiamGia p = phieu();
            p.setGiaTriDonHangToiThieu(v);
            String h = noiDung.html(Loai.MOI, p, khach("An"), false);
            assertTrue(h.contains("Không yêu cầu"), "đơn tối thiểu = " + v);
            assertTrue(noiDung.banChu(Loai.MOI, p, khach("An")).contains("Đơn tối thiểu: Không yêu cầu"));
        }
    }

    @Test
    void mucGiamVaTienGiuDinhDangCu() {
        assertEquals("giảm 10% (tối đa 200.000 ₫)", PhieuMailNoiDung.mucGiam(phieu()));
        assertEquals("1.500.000 ₫", PhieuMailNoiDung.tien(1_500_000L));
        assertEquals("0 ₫", PhieuMailNoiDung.tien(null));
    }

    @Test
    void thieuNgayHoacLoaiGiamKhongLamLoi() {
        PhieuGiamGia p = phieu();
        p.setNgayBatDau(null);
        p.setNgayKetThuc(null);
        p.setLoaiGiam(null);
        for (Loai loai : Loai.values()) {
            assertNotNull(noiDung.html(loai, p, khach("An"), true));
            assertNotNull(noiDung.banChu(loai, p, null));
        }
    }

    /* ---------- chống chèn HTML ---------- */

    @Test
    void tenPhieuLaThe_khongThanhTheHtmlThat() {
        PhieuGiamGia p = phieu();
        p.setTenPhieu("<b>x</b>");
        for (Loai loai : Loai.values()) {
            String h = noiDung.html(loai, p, khach("An"), true);
            assertTrue(h.contains("&lt;b&gt;x&lt;/b&gt;"), "đã escape " + loai);
            assertFalse(h.contains("<b>"), "không có thẻ <b> thật " + loai);
            assertFalse(h.contains("</b>"));
        }
    }

    @Test
    void maPhieuHoTenKhachVaTenCuaHangDeuDuocEscape() {
        PhieuGiamGia p = phieu();
        p.setMaPhieu("A<i>1");
        p.setTenPhieu("Tom & \"Jerry\" 'x'");
        PhieuMailNoiDung nd = new PhieuMailNoiDung("Shop <u>Đẹp</u>", "");
        for (Loai loai : Loai.values()) {
            String h = nd.html(loai, p, khach("<script>alert(1)</script>"), true);
            assertFalse(h.contains("<script"), "không có script " + loai);
            assertFalse(h.contains("<i>"));
            assertFalse(h.contains("<u>"));
            assertTrue(h.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
            assertTrue(h.contains("A&lt;i&gt;1"));
            assertTrue(h.contains("Tom &amp; &quot;Jerry&quot; &#39;x&#39;"));
            assertTrue(h.contains("Shop &lt;u&gt;Đẹp&lt;/u&gt;"));
        }
    }

    @Test
    void chuTiengVietKhongBiDoiThanhMaThucThe() {
        PhieuGiamGia p = phieu();
        p.setTenPhieu("Phiếu Tết Đặc Biệt – Ưu đãi");
        String h = noiDung.html(Loai.MOI, p, khach("Trần Thị Ánh"), false);
        assertTrue(h.contains("Phiếu Tết Đặc Biệt – Ưu đãi"));
        assertTrue(h.contains("Trần Thị Ánh"));
        assertFalse(h.contains("&eacute;") || h.contains("&agrave;") || h.contains("&ocirc;"));
    }

    /* ---------- họ tên khách ---------- */

    @Test
    void hoTenKhachRongHoacThieu_dungQuyKhach() {
        for (String ten : new String[] {null, "", "   "}) {
            KhachHang k = khach(ten);
            for (Loai loai : Loai.values()) {
                assertTrue(noiDung.html(loai, phieu(), k, true).contains("<strong>quý khách</strong>"), "html '" + ten + "' " + loai);
                assertTrue(noiDung.banChu(loai, phieu(), k).startsWith("Xin chào quý khách,"), "chữ '" + ten + "' " + loai);
            }
        }
        assertTrue(noiDung.banChu(Loai.MOI, phieu(), khach("  Lê Hoa ")).startsWith("Xin chào Lê Hoa,"));
    }

    /* ---------- bản chữ ---------- */

    @Test
    void banChuMoiVaCapNhat_duThongTin() {
        String moi = noiDung.banChu(Loai.MOI, phieu(), khach("Nguyễn Văn An"));
        assertTrue(moi.contains("Xin chào Nguyễn Văn An,"));
        assertTrue(moi.contains("tặng bạn một phiếu giảm giá"));
        String capNhat = noiDung.banChu(Loai.CAP_NHAT, phieu(), khach("An"));
        assertTrue(capNhat.contains("đã được cập nhật"));
        assertTrue(capNhat.contains("thay cho thông tin cũ"));
        for (String t : new String[] {moi, capNhat}) {
            assertTrue(t.contains("- Mã phiếu: VCH2ABC"));
            assertTrue(t.contains("- Tên phiếu: Tri ân khách hàng thân thiết"));
            assertTrue(t.contains("- Mức giảm: giảm 10% (tối đa 200.000 ₫)"));
            assertTrue(t.contains("- Đơn tối thiểu: 500.000 ₫"));
            assertTrue(t.contains("- Hiệu lực: 00:00 09/10/2026 – 23:59 15/10/2026"));
            assertTrue(t.contains("Cảm ơn bạn đã đồng hành cùng FootStyle."));
        }
    }

    @Test
    void banChuHuy_coMaVaTenNhungKhongCoMucGiam() {
        String t = noiDung.banChu(Loai.HUY, phieu(), khach("An"));
        assertTrue(t.contains("đã bị hủy và không còn sử dụng được"));
        assertTrue(t.contains("- Mã phiếu: VCH2ABC"));
        assertTrue(t.contains("- Tên phiếu: Tri ân khách hàng thân thiết"));
        assertFalse(t.contains("Mức giảm"));
        assertFalse(t.contains("10%"));
        assertFalse(t.contains("Đơn tối thiểu"));
        assertFalse(t.contains("Hiệu lực"));
    }

    /* ---------- quy tắc cho Gmail ---------- */

    @Test
    void htmlTuanThuQuyTacGmail() {
        for (Loai loai : Loai.values()) {
            String h = html(loai).toLowerCase();
            assertTrue(h.contains("<table"), "bố cục bằng bảng");
            assertTrue(h.contains("max-width:600px"), "rộng tối đa 600px");
            assertTrue(h.contains("width=\"600\""));
            assertTrue(h.contains("align=\"center\""), "căn giữa");
            assertTrue(h.contains("style=\""), "CSS viết thẳng vào thẻ");
            assertFalse(h.contains("<style"), "không dùng <style>");
            assertFalse(h.contains("<script"), "không dùng JavaScript");
            assertFalse(h.contains("<link"), "không dùng font / CSS ngoài");
            assertFalse(h.contains("@import"));
            assertFalse(h.contains("url("));
            assertFalse(h.contains("data:"), "không dùng ảnh data:");
            assertFalse(h.contains("http://"));
            assertFalse(h.contains("https://"), "chưa có link cửa hàng thì không có địa chỉ ngoài nào");
            assertFalse(h.contains("<a "), "chưa có link cửa hàng thì không có nút");
            assertFalse(h.contains("<form"));
            assertFalse(h.contains("onclick"));
        }
    }

    @Test
    void htmlCanBangThe() {
        for (Loai loai : Loai.values()) {
            for (boolean logo : new boolean[] {true, false}) {
                String h = noiDung.html(loai, phieu(), khach("An"), logo);
                for (String the : new String[] {"table", "tr", "td", "div", "span", "h1", "strong", "a", "html", "head", "body"}) {
                    assertEquals(dem(h, "<" + the + "[\\s>]"), dem(h, "</" + the + ">"), "thẻ <" + the + "> " + loai + " logo=" + logo);
                }
            }
        }
    }

    private static int dem(String s, String regex) {
        Matcher m = Pattern.compile(regex).matcher(s);
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    /* ---------- logo ---------- */

    @Test
    void coLogoThiThamChieuCidLogo_khongCoThiKhongCoAnh() {
        String co = noiDung.html(Loai.MOI, phieu(), khach("An"), true);
        assertTrue(co.contains("<img src=\"cid:logo\""));
        assertEquals(1, dem(co, "<img "));
        String khong = noiDung.html(Loai.MOI, phieu(), khach("An"), false);
        assertFalse(khong.contains("<img"));
        assertFalse(khong.contains("cid:"));
        assertTrue(khong.contains(">FootStyle<"), "không logo thì hiện tên cửa hàng bằng chữ");
    }

    /* ---------- nút bấm ---------- */

    @Test
    void coLinkCuaHang_phieuMoiVaCapNhatCoNutHuyKhongCo() {
        PhieuMailNoiDung nd = new PhieuMailNoiDung("FootStyle", "https://footstyle.vn/shop?a=1&b=2");
        for (Loai loai : new Loai[] {Loai.MOI, Loai.CAP_NHAT}) {
            String h = nd.html(loai, phieu(), khach("An"), true);
            assertTrue(h.contains("<a href=\"https://footstyle.vn/shop?a=1&amp;b=2\""), "link đã escape " + loai);
            assertTrue(h.contains("Mua sắm ngay"));
            assertTrue(h.contains("background-color:"), "nút có nền màu");
            assertEquals(1, dem(h, "<a "));
            assertTrue(nd.banChu(loai, phieu(), khach("An")).contains("Mua sắm tại: https://footstyle.vn/shop?a=1&b=2"));
        }
        String huy = nd.html(Loai.HUY, phieu(), khach("An"), true);
        assertFalse(huy.contains("<a "));
        assertFalse(huy.contains("footstyle.vn"));
        assertFalse(nd.banChu(Loai.HUY, phieu(), khach("An")).contains("footstyle.vn"));
    }

    @Test
    void linkKhongPhaiHttpHoacTrongThiKhongCoNut() {
        for (String link : new String[] {null, "", "   ", "javascript:alert(1)", "ftp://x.vn", "footstyle.vn", "https://", "data:text/html,x"}) {
            PhieuMailNoiDung nd = new PhieuMailNoiDung("FootStyle", link);
            assertFalse(nd.html(Loai.MOI, phieu(), khach("An"), true).contains("<a "), "link = " + link);
            assertFalse(nd.banChu(Loai.MOI, phieu(), khach("An")).contains("Mua sắm tại"), "link = " + link);
        }
        assertTrue(new PhieuMailNoiDung("FootStyle", "  HTTPS://footstyle.vn ").html(Loai.MOI, phieu(), khach("An"), true).contains("<a href=\"HTTPS://footstyle.vn\""));
    }

    @Test
    void linkCoDauNhayKepKhongPhaVoThuocTinhHref() {
        PhieuMailNoiDung nd = new PhieuMailNoiDung("FootStyle", "https://x.vn/\" onclick=\"alert(1)");
        String h = nd.html(Loai.MOI, phieu(), khach("An"), true);
        assertFalse(h.contains("\" onclick=\""));
        assertTrue(h.contains("&quot; onclick=&quot;alert(1)"));
    }
}
