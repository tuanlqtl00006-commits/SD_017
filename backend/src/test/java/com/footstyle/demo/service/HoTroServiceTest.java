package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.KhoGia;
import com.footstyle.demo.entity.HoiThoai;
import com.footstyle.demo.entity.TinNhan;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.DotGiamGiaRepository;
import com.footstyle.demo.repository.HoaDonRepository;
import com.footstyle.demo.repository.PhieuGiamGiaRepository;
import com.footstyle.demo.repository.SanPhamChiTietRepository;
import com.footstyle.demo.security.NguoiDung;
import com.footstyle.demo.security.XacThuc;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class HoTroServiceTest {

    private final ChatMau c = new ChatMau();
    private final DongHoChinh dongHo = new DongHoChinh(CaLamMau.HOM_NAY.atTime(9, 0));
    private final HoTroService service;

    /** Trợ lý ảo giả: "gặp người" -> chuyển nhân viên, còn lại trả lời "Đáp: ...". */
    private static final class TroLyGia extends TroLyAoService {
        TroLyGia() {
            super(KhoGia.khongDung(SanPhamChiTietRepository.class), null, KhoGia.khongDung(PhieuGiamGiaRepository.class),
                    KhoGia.khongDung(DotGiamGiaRepository.class), KhoGia.khongDung(HoaDonRepository.class), "FootStyle", "", "", "");
        }

        @Override
        public TraLoi traLoi(String cauHoi, NguCanh nc) {
            if (cauHoi.contains("gặp người")) return new TraLoi("Mình chuyển cho nhân viên nhé.", true, List.of());
            if (cauHoi.contains("gợi ý thẻ")) return new TraLoi("Bấm vào thẻ để xem chi tiết.", false, List.of(), List.of(12, 15));
            return new TraLoi("Đáp: " + cauHoi + " (" + nc.sdtKhach() + ")", false, List.of("Gợi ý A"));
        }
    }

    HoTroServiceTest() {
        service = new HoTroService(c.hoiThoaiRepo(), c.tinNhanRepo(), c.thanhVienRepo(), c.nhanVienRepo(), c.khachRepo(), new TroLyGia());
        service.datDongHo(dongHo);
        c.khach(7, "KH07", "Nguyễn Thị Lan", "0901000001");
    }

    @AfterEach
    void don() {
        XacThuc.xoa();
    }

    private Map<String, Object> batDau(String ten, String sdt) {
        return service.batDau(new HoTroService.BatDauRequest(ten, sdt));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> tin(Map<String, Object> m) {
        return (List<Map<String, Object>>) m.get("tinNhan");
    }

    private HoiThoai hoiThoai(Object id) {
        return c.hoiThoais.stream().filter(h -> h.getId().equals(id)).findFirst().orElseThrow();
    }

    /* ---------------- phía khách ---------------- */

    @Test
    void batDau_kiemTraTungO() {
        ApiException e = assertThrows(ApiException.class, () -> batDau("  ", ""));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        assertEquals("Hãy nhập họ tên.", e.getLoiTheoTruong().get("hoTen"));
        assertTrue(e.getLoiTheoTruong().get("sdt").startsWith("Hãy nhập số điện thoại"));
        e = assertThrows(ApiException.class, () -> batDau("Lan 123", "012345"));
        assertEquals(QuyTacDuLieu.LOI_HO_TEN_KY_TU, e.getLoiTheoTruong().get("hoTen"));
        assertEquals(QuyTacDuLieu.LOI_SDT, e.getLoiTheoTruong().get("sdt"));
        assertThrows(ApiException.class, () -> service.batDau(null));
        assertTrue(c.hoiThoais.isEmpty());
    }

    @Test
    void batDau_taoHoiThoaiTroLyAo_coMaBiMat_vaLoiChao() {
        Map<String, Object> kq = batDau("  nguyễn   thị lan ", "0901 000 001");
        HoiThoai h = hoiThoai(kq.get("id"));
        assertEquals(HoiThoai.HO_TRO_KHACH, h.getLoai());
        assertEquals(HoiThoai.TRO_LY_AO, kq.get("cheDo"));
        assertEquals("nguyễn thị lan", h.getTenKhach());
        assertEquals("0901000001", h.getSdtKhach());
        assertEquals(7, h.getIdKhachHang(), "SĐT trùng khách đã có -> gắn khách hàng");
        String ma = (String) kq.get("ma");
        assertEquals(32, ma.length());
        assertTrue(ma.matches("[A-Za-z0-9_-]+"));
        assertEquals(ma, h.getMaTruyCap());
        List<Map<String, Object>> t = tin(kq);
        assertEquals(1, t.size());
        assertEquals(TinNhan.TRO_LY_AO, t.get(0).get("loaiNguoiGui"));
        assertTrue(((String) t.get(0).get("noiDung")).startsWith("Xin chào nguyễn thị lan!"));
        assertFalse(t.get(0).containsKey("idNhanVien"), "khách không thấy id nhân viên");
        assertEquals(TroLyAoService.GOI_Y_MAC_DINH, kq.get("goiY"));
        // 2 cuộc chat khác nhau có mã khác nhau
        assertFalse(ma.equals(batDau("Lan", "0901000001").get("ma")));
        // SĐT lạ: không gắn khách
        assertNull(hoiThoai(batDau("Hùng", "0912345678").get("id")).getIdKhachHang());
    }

    @Test
    void batDau_khachDaDangNhap_ganDungTaiKhoan() {
        XacThuc.dat(new NguoiDung(NguoiDung.LOAI_KHACH_HANG, 42, "KH42", "Hùng", NguoiDung.KHACH_HANG, null));
        assertEquals(42, hoiThoai(batDau("Hùng", "0901000001").get("id")).getIdKhachHang());
    }

    @Test
    void batDau_moiSdtToiDa5CuocMoiGio() {
        for (int i = 0; i < HoTroService.SO_HOI_THOAI_MOI_GIO; i++) batDau("Lan", "0901000001");
        ApiException e = assertThrows(ApiException.class, () -> batDau("Lan", "0901000001"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getStatus());
        assertEquals("QUA_NHIEU_YEU_CAU", e.getCode());
        batDau("Hùng", "0912345678"); // SĐT khác vẫn được
        dongHo.tien(3601);
        batDau("Lan", "0901000001"); // sau 1 giờ lại được
    }

    @Test
    void xemKhach_saiMaHoacSaiIdDeuKhongTimThay() {
        Map<String, Object> kq = batDau("Lan", "0901000001");
        Integer id = (Integer) kq.get("id");
        for (String ma : new String[] {null, "", "sai-ma", ((String) kq.get("ma")).toUpperCase()}) {
            ApiException e = assertThrows(ApiException.class, () -> service.xemKhach(id, ma, null));
            assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
            assertEquals("KHONG_TIM_THAY_HOI_THOAI", e.getCode());
        }
        assertEquals("KHONG_TIM_THAY_HOI_THOAI",
                assertThrows(ApiException.class, () -> service.xemKhach(999, (String) kq.get("ma"), null)).getCode());
        Map<String, Object> xem = service.xemKhach(id, (String) kq.get("ma"), null);
        assertEquals(1, tin(xem).size());
        assertEquals(TroLyAoService.GOI_Y_MAC_DINH, xem.get("goiY"));
        assertNull(xem.get("ma"), "mã chỉ trả lúc bắt đầu");
    }

    @Test
    void guiKhach_troLyTraLoi_vaChuyenNhanVienKhiCan() {
        Map<String, Object> kq = batDau("Lan", "0901000001");
        Integer id = (Integer) kq.get("id");
        String ma = (String) kq.get("ma");
        Map<String, Object> r = service.guiKhach(id, new HoTroService.TinKhachRequest(ma, "  vợt yonex  "));
        List<Map<String, Object>> t = tin(r);
        assertEquals(2, t.size());
        assertEquals(TinNhan.KHACH, t.get(0).get("loaiNguoiGui"));
        assertEquals("vợt yonex", t.get(0).get("noiDung"));
        assertEquals("Lan", t.get(0).get("tenNguoiGui"));
        assertEquals("Đáp: vợt yonex (0901000001)", t.get(1).get("noiDung"), "trợ lý biết SĐT khách để tra đơn");
        assertEquals(List.of("Gợi ý A"), r.get("goiY"));
        assertEquals(HoiThoai.TRO_LY_AO, r.get("cheDo"));

        r = service.guiKhach(id, new HoTroService.TinKhachRequest(ma, "cho gặp người thật"));
        assertEquals(HoiThoai.CHO_NHAN_VIEN, r.get("cheDo"));
        // Đang chờ nhân viên: trợ lý im lặng, tin của khách chờ nhân viên đọc
        r = service.guiKhach(id, new HoTroService.TinKhachRequest(ma, "alo"));
        assertEquals(1, tin(r).size());
        assertEquals(6, service.xemKhach(id, ma, null).get("tinNhan") instanceof List<?> l ? l.size() : -1);
        // hỏi lại định kỳ: chỉ lấy tin mới hơn sauId
        Integer idCuoi = (Integer) tin(r).get(0).get("id");
        assertTrue(tin(service.xemKhach(id, ma, idCuoi)).isEmpty());
        assertEquals(1, tin(service.xemKhach(id, ma, idCuoi - 1)).size());
    }

    @Test
    void guiKhach_noiDungRongHoacQuaDai_vaChongSpam() {
        Map<String, Object> kq = batDau("Lan", "0901000001");
        Integer id = (Integer) kq.get("id");
        String ma = (String) kq.get("ma");
        ApiException e = assertThrows(ApiException.class, () -> service.guiKhach(id, new HoTroService.TinKhachRequest(ma, "   ")));
        assertEquals("Hãy nhập nội dung tin nhắn.", e.getLoiTheoTruong().get("noiDung"));
        e = assertThrows(ApiException.class, () -> service.guiKhach(id, new HoTroService.TinKhachRequest(ma, "a".repeat(1001))));
        assertTrue(e.getLoiTheoTruong().get("noiDung").startsWith("Tin nhắn tối đa 1000 ký tự"));
        service.guiKhach(id, new HoTroService.TinKhachRequest(ma, "a".repeat(1000)));
        for (int i = 1; i < HoTroService.SO_TIN_KHACH_MOI_PHUT; i++) service.guiKhach(id, new HoTroService.TinKhachRequest(ma, "tin " + i));
        e = assertThrows(ApiException.class, () -> service.guiKhach(id, new HoTroService.TinKhachRequest(ma, "nữa")));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getStatus());
        dongHo.tien(61);
        service.guiKhach(id, new HoTroService.TinKhachRequest(ma, "được rồi"));
        assertThrows(ApiException.class, () -> service.guiKhach(id, null));
    }

    @Test
    void gapNhanVien_vaKetThuc_khongLapTin() {
        Map<String, Object> kq = batDau("Lan", "0901000001");
        Integer id = (Integer) kq.get("id");
        String ma = (String) kq.get("ma");
        Map<String, Object> r = service.gapNhanVien(id, new HoTroService.MaRequest(ma));
        assertEquals(HoiThoai.CHO_NHAN_VIEN, r.get("cheDo"));
        assertEquals(TinNhan.HE_THONG, tin(r).get(0).get("loaiNguoiGui"));
        assertTrue(tin(service.gapNhanVien(id, new HoTroService.MaRequest(ma))).isEmpty(), "bấm lần 2 không thêm tin");
        assertThrows(ApiException.class, () -> service.gapNhanVien(id, new HoTroService.MaRequest("sai")));

        r = service.ketThucKhach(id, new HoTroService.MaRequest(ma));
        assertEquals(HoiThoai.DA_DONG, r.get("cheDo"));
        assertEquals("Khách đã kết thúc cuộc trò chuyện.", tin(r).get(0).get("noiDung"));
        assertTrue(tin(service.ketThucKhach(id, new HoTroService.MaRequest(ma))).isEmpty());
        ApiException e = assertThrows(ApiException.class, () -> service.guiKhach(id, new HoTroService.TinKhachRequest(ma, "còn đó không")));
        assertEquals("DA_KET_THUC", e.getCode());
        assertEquals("DA_KET_THUC", assertThrows(ApiException.class, () -> service.gapNhanVien(id, new HoTroService.MaRequest(ma))).getCode());
        assertNotNull(service.xemKhach(id, ma, null), "đã đóng vẫn xem lại được");
    }

    /* ---------------- phía nhân viên ---------------- */

    @Test
    void nhanVien_phaiDangNhap() {
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(ApiException.class, () -> service.danhSach(null)).getStatus());
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(ApiException.class, () -> service.xem(1, null)).getStatus());
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(ApiException.class, () -> service.gui(1, new HoTroService.NoiDungRequest("x"))).getStatus());
    }

    @Test
    void luongDayDu_choNhanVien_nhan_traLoi_dong() {
        Map<String, Object> kq = batDau("Lan", "0901000001");
        Integer id = (Integer) kq.get("id");
        String ma = (String) kq.get("ma");
        batDau("Hùng", "0912345678"); // cuộc khác, vẫn ở trợ lý ảo
        service.gapNhanVien(id, new HoTroService.MaRequest(ma));
        service.guiKhach(id, new HoTroService.TinKhachRequest(ma, "shop ơi"));

        ChatMau.dangNhap(c.m.bao);
        Map<String, Object> ds = service.danhSach(null);
        @SuppressWarnings("unchecked")
        Map<String, Integer> dem = (Map<String, Integer>) ds.get("dem");
        assertEquals(2, dem.get("tatCa"));
        assertEquals(1, dem.get("cho"));
        assertEquals(1, dem.get("troLy"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cho = (List<Map<String, Object>>) service.danhSach("cho").get("hoiThoai");
        assertEquals(1, cho.size());
        assertEquals(id, cho.get(0).get("id"));
        assertEquals(3L, cho.get(0).get("chuaDoc"), "lời chào + tin hệ thống + tin khách đều chưa đọc");
        assertEquals("shop ơi", ((Map<?, ?>) cho.get(0).get("tinCuoi")).get("noiDung"));
        assertEquals(1, service.soCanXuLy(c.m.bao.getId()));

        // Xem: thấy cả id nhân viên + thông tin khách đã có tài khoản; đánh dấu đã đọc
        Map<String, Object> xem = service.xem(id, null);
        assertEquals("KH07", ((Map<?, ?>) xem.get("khachHang")).get("ma"));
        assertTrue(tin(xem).get(0).containsKey("idNhanVien"));
        assertEquals(1, service.soCanXuLy(c.m.bao.getId()), "đang chờ nhân viên thì vẫn tính cần xử lý dù đã đọc");

        // Trả lời luôn = tự nhận hỗ trợ
        Map<String, Object> t = service.gui(id, new HoTroService.NoiDungRequest("Chào chị, em Bảo đây ạ"));
        assertEquals(TinNhan.NHAN_VIEN, t.get("loaiNguoiGui"));
        assertEquals(c.m.bao.getId(), t.get("idNhanVien"));
        assertEquals("Lê Quốc Bảo", t.get("tenNguoiGui"));
        HoiThoai h = hoiThoai(id);
        assertEquals(HoiThoai.NHAN_VIEN_HO_TRO, h.getCheDo());
        assertEquals(c.m.bao.getId(), h.getIdNhanVienPhuTrach());
        Map<String, Object> phiaKhach = service.xemKhach(id, ma, null);
        assertEquals("Lê Quốc Bảo", phiaKhach.get("tenNhanVien"));
        List<Map<String, Object>> tinKhach = tin(phiaKhach);
        assertEquals("Nhân viên Lê Quốc Bảo đã tham gia hỗ trợ bạn.", tinKhach.get(tinKhach.size() - 2).get("noiDung"));
        assertEquals(0, service.soCanXuLy(c.m.bao.getId()));

        // Khách nhắn tiếp: trợ lý không chen vào; nhân viên phụ trách có 1 tin chưa đọc
        assertEquals(1, tin(service.guiKhach(id, new HoTroService.TinKhachRequest(ma, "vợt 4U còn không em"))).size());
        assertEquals(1, service.soCanXuLy(c.m.bao.getId()));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cuaToi = (List<Map<String, Object>>) service.danhSach("cua-toi").get("hoiThoai");
        assertEquals(1L, cuaToi.get(0).get("chuaDoc"));
        service.xem(id, null);
        assertEquals(0, service.soCanXuLy(c.m.bao.getId()));

        // Nhân viên khác: không chen vào, không đóng được, không thấy trong "của tôi", không tính cần xử lý
        ChatMau.dangNhap(c.m.anh);
        ApiException e = assertThrows(ApiException.class, () -> service.gui(id, new HoTroService.NoiDungRequest("để em")));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        assertEquals("NGUOI_KHAC_HO_TRO", e.getCode());
        assertTrue(e.getMessage().contains("Lê Quốc Bảo"));
        assertEquals("NGUOI_KHAC_HO_TRO", assertThrows(ApiException.class, () -> service.nhan(id)).getCode());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ApiException.class, () -> service.dong(id)).getStatus());
        assertEquals(0, service.soCanXuLy(c.m.anh.getId()));
        assertEquals(0, ((List<?>) service.danhSach("cua-toi").get("hoiThoai")).size());

        // Quản lý: gửi thẳng thì bị chặn, bấm "Nhận hỗ trợ" thì tiếp quản
        ChatMau.dangNhap(c.m.quanLy);
        e = assertThrows(ApiException.class, () -> service.gui(id, new HoTroService.NoiDungRequest("x")));
        assertTrue(e.getMessage().contains("Nhận hỗ trợ"));
        Map<String, Object> nhan = service.nhan(id);
        assertEquals(c.m.quanLy.getId(), nhan.get("idNhanVienPhuTrach"));
        List<Map<String, Object>> tinQl = tin(nhan);
        assertEquals("Nhân viên Nguyễn Hoàng Long đã tiếp quản cuộc trò chuyện từ Lê Quốc Bảo.", tinQl.get(tinQl.size() - 1).get("noiDung"));
        service.nhan(id); // nhận lại khi đang là mình: không thêm tin
        assertEquals(tinQl.size(), tin(service.xem(id, null)).size());

        // Kết thúc
        Map<String, Object> dong = service.dong(id);
        assertEquals(HoiThoai.DA_DONG, dong.get("cheDo"));
        assertEquals("DA_KET_THUC", assertThrows(ApiException.class, () -> service.gui(id, new HoTroService.NoiDungRequest("x"))).getCode());
        assertEquals("DA_KET_THUC", assertThrows(ApiException.class, () -> service.nhan(id)).getCode());
        service.dong(id); // đóng lần 2 không lỗi
        assertEquals(1, ((List<?>) service.danhSach("da-dong").get("hoiThoai")).size());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ApiException.class, () -> service.xem(999, null)).getStatus());
    }

    @Test
    void nhanVien_dongCuocDangChoNhanVien_duoc() {
        Map<String, Object> kq = batDau("Lan", "0901000001");
        Integer id = (Integer) kq.get("id");
        ChatMau.dangNhap(c.m.anh);
        assertEquals(HoiThoai.DA_DONG, service.dong(id).get("cheDo"));
    }

    @Test
    void dungMa() {
        assertTrue(HoTroService.dungMa("abc", "abc"));
        assertFalse(HoTroService.dungMa("abc", "abd"));
        assertFalse(HoTroService.dungMa("abc", null));
        assertFalse(HoTroService.dungMa(null, "abc"));
    }

    /* ---------------- thẻ sản phẩm, tự đóng phiên, đánh giá (cô review 14/11 [20:24–23:17]) ---------------- */

    @Test
    void troLyGoiYSanPham_luuKemDanhSachThe() {
        Map<String, Object> kq = batDau("Lan", "0901000001");
        Map<String, Object> tl = service.guiKhach((Integer) kq.get("id"), new HoTroService.TinKhachRequest((String) kq.get("ma"), "gợi ý thẻ giúp mình"));
        assertEquals("Bấm vào thẻ để xem chi tiết.\n[[SP:12,15]]", tin(tl).get(1).get("noiDung"));
    }

    @Test
    void tuDongDong_sau30PhutKhongCoTinMoi_coNhanVienThiMoiDanhGia() {
        Map<String, Object> a = batDau("Lan", "0901000001");
        Integer idA = (Integer) a.get("id");
        String maA = (String) a.get("ma");
        service.gapNhanVien(idA, new HoTroService.MaRequest(maA));
        ChatMau.dangNhap(c.m.bao);
        service.gui(idA, new HoTroService.NoiDungRequest("Chào chị, em hỗ trợ ạ")); // nhân viên nhận
        XacThuc.xoa();
        Integer idB = (Integer) batDau("Hùng", "0912345678").get("id"); // chỉ trợ lý ảo

        dongHo.tien(29 * 60);
        assertEquals(0, service.tuDongDongPhienRanh(), "chưa đủ 30 phút");
        dongHo.tien(60);
        assertEquals(2, service.tuDongDongPhienRanh());
        assertEquals(HoiThoai.DA_DONG, hoiThoai(idA).getCheDo());
        assertEquals(HoiThoai.DA_DONG, hoiThoai(idB).getCheDo());

        Map<String, Object> xemA = service.xemKhach(idA, maA, null);
        assertTrue(String.valueOf(tin(xemA).get(tin(xemA).size() - 1).get("noiDung")).contains("không có tin nhắn mới trong 30 phút"));
        assertEquals(true, xemA.get("canDanhGia"), "có nhân viên tham gia: mời đánh giá");
        assertEquals(false, service.xemKhach(idB, hoiThoai(idB).getMaTruyCap(), null).get("canDanhGia"),
                "chỉ trợ lý ảo: không cần đánh giá");
        assertEquals(0, service.tuDongDongPhienRanh(), "đã đóng thì không đóng lại");
    }

    @Test
    void danhGia_sauKhiKetThuc_motLan_luuThanhTinHeThong() {
        Map<String, Object> kq = batDau("Lan", "0901000001");
        Integer id = (Integer) kq.get("id");
        String ma = (String) kq.get("ma");
        assertThrows(ApiException.class, () -> service.danhGia(id, new HoTroService.DanhGiaRequest(ma, "TOT", true, null)), "chưa kết thúc");
        service.gapNhanVien(id, new HoTroService.MaRequest(ma));
        ChatMau.dangNhap(c.m.bao);
        service.gui(id, new HoTroService.NoiDungRequest("Em chào chị"));
        service.dong(id);
        XacThuc.xoa();

        ApiException e = assertThrows(ApiException.class, () -> service.danhGia(id, new HoTroService.DanhGiaRequest(ma, "SAI", null, null)));
        assertEquals("Hãy chọn mức độ hài lòng.", e.getLoiTheoTruong().get("mucDo"));
        assertTrue(e.getLoiTheoTruong().containsKey("daGiaiQuyet"));
        Map<String, Object> sau = service.danhGia(id, new HoTroService.DanhGiaRequest(ma, "tot", true, "  Nhân viên nhiệt tình "));
        assertEquals("Khách đánh giá: Tốt · Đã giải quyết yêu cầu · Góp ý: Nhân viên nhiệt tình", tin(sau).get(0).get("noiDung"));
        assertEquals(false, sau.get("canDanhGia"));
        assertEquals(true, sau.get("daDanhGia"));
        assertThrows(ApiException.class, () -> service.danhGia(id, new HoTroService.DanhGiaRequest(ma, "CHUA_TOT", false, null)), "chỉ 1 lần");
        // sai mã truy cập thì không đánh giá hộ người khác được
        assertThrows(ApiException.class, () -> service.danhGia(id, new HoTroService.DanhGiaRequest("sai", "TOT", true, null)));
    }

    /** Một cuộc chat khách có nhân viên `nv` hỗ trợ, đã kết thúc và được khách đánh giá. */
    private Integer chatDaDanhGia(String ten, String sdt, com.footstyle.demo.entity.NhanVien nv, String muc, boolean daGiaiQuyet, String gopY) {
        Map<String, Object> kq = batDau(ten, sdt);
        Integer id = (Integer) kq.get("id");
        String ma = (String) kq.get("ma");
        service.gapNhanVien(id, new HoTroService.MaRequest(ma));
        ChatMau.dangNhap(nv);
        service.gui(id, new HoTroService.NoiDungRequest("Em chào anh/chị"));
        service.dong(id);
        XacThuc.xoa();
        service.danhGia(id, new HoTroService.DanhGiaRequest(ma, muc, daGiaiQuyet, gopY));
        return id;
    }

    /** Góp ý 16/11 [12:30–12:59]: trang tổng hợp đánh giá tốt / xấu sau chat, đọc lại từ tin "Khách đánh giá: ...". */
    @Test
    void tongHopDanhGia_demTheoMuc_theoNhanVien_locMucVaNhanVien() {
        Integer id1 = chatDaDanhGia("Trần An", "0901000011", c.m.bao, "TOT", true, "Nhiệt tình · nhanh");
        chatDaDanhGia("Lê Bình", "0901000012", c.m.bao, "CHUA_TOT", false, null);
        chatDaDanhGia("Phạm Chi", "0901000013", c.m.anh, "BINH_THUONG", true, null);
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(ApiException.class, () -> service.tongHopDanhGia(null, null, null, null)).getStatus(),
                "khách / chưa đăng nhập không xem được");

        ChatMau.dangNhap(c.m.bao);
        Map<String, Object> kq = service.tongHopDanhGia(null, null, null, null);
        assertEquals(3, kq.get("tong"));
        assertEquals(Map.of("TOT", 1, "BINH_THUONG", 1, "CHUA_TOT", 1), kq.get("dem"));
        assertEquals(2, kq.get("daGiaiQuyet"));
        assertEquals(1, kq.get("chuaGiaiQuyet"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ds = (List<Map<String, Object>>) kq.get("danhGia");
        Map<String, Object> tot = ds.stream().filter(d -> id1.equals(d.get("idHoiThoai"))).findFirst().orElseThrow();
        assertEquals("Tốt", tot.get("nhanMucDo"));
        assertEquals("Nhiệt tình · nhanh", tot.get("gopY"), "góp ý có dấu · vẫn đọc đủ");
        assertEquals(c.m.bao.getHoTen(), tot.get("tenNhanVien"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> theoNv = (List<Map<String, Object>>) kq.get("theoNhanVien");
        Map<String, Object> bao = theoNv.stream().filter(n -> c.m.bao.getId().equals(n.get("idNhanVien"))).findFirst().orElseThrow();
        assertEquals(2, bao.get("tong"));
        assertEquals(1, bao.get("CHUA_TOT"));

        assertEquals(1, service.tongHopDanhGia(null, null, "chua_tot", null).get("tong"));
        Map<String, Object> cuaAnh = service.tongHopDanhGia(null, null, null, c.m.anh.getId());
        assertEquals(1, cuaAnh.get("tong"));
        assertEquals(2, ((List<?>) cuaAnh.get("nhanVien")).size(), "ô lọc nhân viên vẫn đủ người dù đang lọc");
        assertEquals(0, service.tongHopDanhGia(CaLamMau.HOM_NAY.plusDays(1), null, null, null).get("tong"), "lọc theo ngày đánh giá");
        assertEquals(3, service.tongHopDanhGia(CaLamMau.HOM_NAY, CaLamMau.HOM_NAY, null, null).get("tong"));
        assertThrows(ApiException.class, () -> service.tongHopDanhGia(null, null, "SAI", null));
        assertThrows(ApiException.class, () -> service.tongHopDanhGia(CaLamMau.HOM_NAY, CaLamMau.HOM_NAY.minusDays(1), null, null));
    }

    @Test
    void docDanhGia_saiDangThiBoQua() {
        assertEquals(new HoTroService.DanhGiaDoc("CHUA_TOT", false, null),
                HoTroService.docDanhGia("Khách đánh giá: Chưa tốt · Chưa giải quyết yêu cầu"));
        assertNull(HoTroService.docDanhGia("Khách đánh giá: Tuyệt vời · Đã giải quyết yêu cầu"));
        assertNull(HoTroService.docDanhGia("Nhân viên đã kết thúc"));
    }
}
