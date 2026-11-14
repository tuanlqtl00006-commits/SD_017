package com.footstyle.demo.service;

import static com.footstyle.demo.service.ChuDeSuKien.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.config.SuKienFilter;
import com.footstyle.demo.security.PhanQuyen;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Đồng bộ dữ liệu giữa các màn: ghi qua API nào thì báo chủ đề nào, chỉ báo khi ghi thành công. */
class ChuDeSuKienTest {

    @Test
    void banQuay_datOnline_doiTrangThaiDon_baoHoaDonVaTonKho() {
        for (String[] r : new String[][] {
                {"POST", "/api/ban-hang/hoa-don-cho/5/thanh-toan"},
                {"POST", "/api/cua-hang/dat-hang"},
                {"PUT", "/api/hoa-don/12/trang-thai"},
                {"PUT", "/api/khach/don-hang/HD12/huy"}}) {
            Set<String> cd = ChuDeSuKien.theoYeuCau(r[0], r[1]);
            assertTrue(cd.contains(HOA_DON) && cd.contains(TON_KHO), r[1] + " -> " + cd);
        }
        assertTrue(ChuDeSuKien.theoYeuCau("POST", "/api/ban-hang/hoa-don-cho").contains(GIAO_CA), "bán quầy đổi số liệu ca");
    }

    @Test
    void sanPham_giamGia_phieu_khach_nhanVien_ca_lich() {
        assertEquals(Set.of(SAN_PHAM, TON_KHO), ChuDeSuKien.theoYeuCau("PUT", "/api/bien-the-san-pham/3"));
        assertEquals(Set.of(SAN_PHAM, TON_KHO), ChuDeSuKien.theoYeuCau("POST", "/api/san-pham"));
        assertTrue(ChuDeSuKien.theoYeuCau("POST", "/api/dot-giam-gia").containsAll(Set.of(GIAM_GIA, SAN_PHAM)), "đợt giảm đổi giá bán");
        assertEquals(Set.of(PHIEU_GIAM_GIA), ChuDeSuKien.theoYeuCau("DELETE", "/api/phieu-giam-gia/4"));
        assertEquals(Set.of(KHACH_HANG), ChuDeSuKien.theoYeuCau("POST", "/api/khach-hang"));
        assertEquals(Set.of(NHAN_VIEN), ChuDeSuKien.theoYeuCau("PUT", "/api/nhan-vien/2"));
        assertEquals(Set.of(GIAO_CA), ChuDeSuKien.theoYeuCau("POST", "/api/giao-ca/nhan-ca"));
        assertTrue(ChuDeSuKien.theoYeuCau("POST", "/api/lich-lam-viec/xep").contains(LICH_LAM_VIEC));
        // quản lý đổi ngưỡng miễn phí vận chuyển: màn quầy, giỏ hàng tải lại cài đặt
        assertEquals(Set.of(ChuDeSuKien.CAI_DAT), ChuDeSuKien.theoYeuCau("PUT", "/api/cai-dat/van-chuyen"));
        assertEquals(Set.of(), ChuDeSuKien.theoYeuCau("GET", "/api/cua-hang/cai-dat-van-chuyen"));
        // /api/khach (khách đăng nhập) không nhầm với /api/khach-hang (quản lý khách)
        assertTrue(ChuDeSuKien.theoYeuCau("PUT", "/api/khach/tai-khoan").contains(HOA_DON));
        assertTrue(!ChuDeSuKien.theoYeuCau("PUT", "/api/khach-hang/7").contains(HOA_DON));
        // không phân biệt hoa thường, dấu / cuối, tham số
        assertEquals(Set.of(KHACH_HANG), ChuDeSuKien.theoYeuCau("post", "/API/Khach-Hang/?x=1"));
    }

    @Test
    void docDuLieu_hoacGhiKhongDoiDuLieu_khongBao() {
        assertTrue(ChuDeSuKien.theoYeuCau("GET", "/api/hoa-don").isEmpty());
        assertTrue(ChuDeSuKien.theoYeuCau("OPTIONS", "/api/hoa-don").isEmpty());
        for (String p : List.of("/api/auth/dang-nhap", "/api/cua-hang/tinh-tien", "/api/tro-ly/hoi", "/api/thong-ke/gui-email",
                "/api/chat/tin-nhan", "/api/ho-tro/khach/1/tin-nhan", "/api/hinh-anh/upload", "/api/su-kien", "/api/khong-co")) {
            assertTrue(ChuDeSuKien.theoYeuCau("POST", p).isEmpty(), p);
        }
        assertTrue(ChuDeSuKien.theoYeuCau("DELETE", "/api/hinh-anh/9").contains(SAN_PHAM), "xóa ảnh đã lưu thì báo");
    }

    @Test
    void kenhSuKien_congKhai_khongCanVaoCa() {
        assertTrue(PhanQuyen.congKhai("GET", "/api/su-kien"));
    }

    /** SuKienService giả: ghi lại các lần báo. */
    static class SuKienGhiLai extends SuKienService {
        final List<Object[]> daBao = new ArrayList<>();

        @Override
        public void bao(Set<String> chuDe, String nguon) {
            daBao.add(new Object[] {chuDe, nguon});
        }
    }

    private static List<Object[]> chay(String method, String uri, int maTraVe, String tab) throws Exception {
        SuKienGhiLai suKien = new SuKienGhiLai();
        SuKienFilter loc = new SuKienFilter(suKien);
        MockHttpServletRequest req = new MockHttpServletRequest(method, uri);
        if (tab != null) req.addHeader(SuKienFilter.TIEU_DE_TAB, tab);
        MockHttpServletResponse res = new MockHttpServletResponse();
        loc.doFilter(req, res, (rq, rs) -> ((MockHttpServletResponse) rs).setStatus(maTraVe));
        return suKien.daBao;
    }

    @Test
    void boLoc_chiBaoKhiGhiThanhCong_kemMaTab() throws Exception {
        List<Object[]> ok = chay("POST", "/api/ban-hang/hoa-don-cho/5/thanh-toan", 200, "tab-1");
        assertEquals(1, ok.size());
        assertTrue(((Set<?>) ok.get(0)[0]).contains(TON_KHO));
        assertEquals("tab-1", ok.get(0)[1]);

        assertTrue(chay("POST", "/api/ban-hang/hoa-don-cho/5/thanh-toan", 409, null).isEmpty(), "lỗi nghiệp vụ: dữ liệu không đổi");
        assertTrue(chay("PUT", "/api/hoa-don/1", 500, null).isEmpty());
        assertTrue(chay("GET", "/api/hoa-don", 200, null).isEmpty(), "chỉ đọc");
        assertTrue(chay("POST", "/api/auth/dang-nhap", 200, null).isEmpty(), "đăng nhập không đổi dữ liệu");
    }

    @Test
    void dichVu_khongCoKetNoi_baoKhongLoi_vaDongDuoc() {
        SuKienService s = new SuKienService();
        s.bao(Set.of(HOA_DON), "x"); // chưa ai nghe: bỏ qua
        s.bao(Set.of(), null);
        s.nhipTim();
        assertEquals(0, s.soKetNoi());
        s.moKetNoi();
        s.moKetNoi();
        assertEquals(2, s.soKetNoi());
        s.bao(Set.of(HOA_DON), "<script>"); // mã tab lạ bị bỏ, không lỗi
        s.dongTatCa();
        assertEquals(0, s.soKetNoi());
        s.bao(Set.of(HOA_DON), null); // đã tắt: không lỗi
    }
}
