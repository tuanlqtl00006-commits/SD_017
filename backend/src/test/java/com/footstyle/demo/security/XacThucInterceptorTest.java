package com.footstyle.demo.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class XacThucInterceptorTest {

    private final TaiKhoanMau mau = new TaiKhoanMau();
    private final XacThucInterceptor it = mau.interceptor(true);

    private static String bearer(String t) {
        return "Bearer " + t;
    }

    @Test
    void preflightVaDuongDanCongKhai_khongCanToken() {
        assertEquals(0, it.xacThuc("OPTIONS", "/api/nhan-vien", null).status());
        assertEquals(0, it.xacThuc("POST", "/api/auth/dang-nhap", null).status());
        assertEquals(0, it.xacThuc("GET", "/api/uploads/a.png", null).status());
        assertNull(it.xacThuc("GET", "/api/uploads/a.png", null).nguoiDung());
    }

    @Test
    void chuaDangNhap_401() {
        XacThucInterceptor.KetQua kq = it.xacThuc("GET", "/api/hoa-don", null);
        assertEquals(401, kq.status());
        assertEquals("CHUA_DANG_NHAP", kq.code());
        assertEquals(401, it.xacThuc("GET", "/api/hoa-don", "Basic abc").status());
        assertEquals(401, it.xacThuc("GET", "/api/hoa-don", "Bearer ").status());
    }

    @Test
    void tokenRacHoacGiaMao_401() {
        XacThucInterceptor.KetQua kq = it.xacThuc("GET", "/api/hoa-don", bearer("abc.def"));
        assertEquals(401, kq.status());
        assertEquals("TOKEN_KHONG_HOP_LE", kq.code());
    }

    @Test
    void tokenHetHan_401_HET_PHIEN() {
        TokenXacThuc cu = new TokenXacThuc("chuoi-bi-mat-de-test-123456", Duration.ofHours(12),
                Clock.fixed(Instant.now().minus(Duration.ofDays(1)), ZoneId.of("UTC")));
        String t = cu.tao("NV", 3, mau.nhanVien.getMatKhau());
        XacThucInterceptor.KetQua kq = it.xacThuc("GET", "/api/hoa-don", bearer(t));
        assertEquals(401, kq.status());
        assertEquals("HET_PHIEN", kq.code());
    }

    @Test
    void nhanVienHopLe_vaoDuocHoaDon_khongVaoDuocNhanVien() {
        String t = mau.tokenCua(mau.nhanVien);
        XacThucInterceptor.KetQua ok = it.xacThuc("GET", "/api/hoa-don", bearer(t));
        assertEquals(0, ok.status());
        assertEquals("Lê Quốc Bảo", ok.nguoiDung().hoTen());
        assertEquals(NguoiDung.NHAN_VIEN, ok.nguoiDung().vaiTro());
        XacThucInterceptor.KetQua cam = it.xacThuc("GET", "/api/nhan-vien", bearer(t));
        assertEquals(403, cam.status());
        assertEquals("KHONG_DU_QUYEN", cam.code());
    }

    @Test
    void quanLy_vaoDuocMoiThu() {
        String t = mau.tokenCua(mau.quanLy);
        XacThucInterceptor.KetQua kq = it.xacThuc("DELETE", "/api/khach-hang/2", bearer(t));
        assertEquals(0, kq.status());
        assertEquals(NguoiDung.QUAN_LY, kq.nguoiDung().vaiTro());
    }

    @Test
    void nhanVienBiNgungHoatDongSauKhiDangNhap_bichanNgay() {
        String t = mau.tokenCua(mau.nhanVien);
        mau.nhanVien.setTrangThai(0);
        XacThucInterceptor.KetQua kq = it.xacThuc("GET", "/api/hoa-don", bearer(t));
        assertEquals(401, kq.status());
        assertEquals("HET_PHIEN", kq.code());
    }

    @Test
    void doiMatKhau_tokenCuHetHieuLuc() {
        String t = mau.tokenCua(mau.nhanVien);
        mau.nhanVien.setMatKhau(mau.encoder.encode("MatKhauMoi1"));
        assertEquals(401, it.xacThuc("GET", "/api/hoa-don", bearer(t)).status());
        assertEquals(0, it.xacThuc("GET", "/api/hoa-don", bearer(mau.tokenCua(mau.nhanVien))).status());
    }

    @Test
    void tokenKhachHang_khongVaoDuocTrangQuanTri() {
        String t = mau.token.tao(NguoiDung.LOAI_KHACH_HANG, 5, mau.khach.getMatKhau());
        assertEquals(0, it.xacThuc("GET", "/api/auth/toi", bearer(t)).status());
        assertEquals(403, it.xacThuc("GET", "/api/hoa-don", bearer(t)).status());
    }

    @Test
    void duongDanCongKhai_coTokenThiVanBietAiGoi() {
        String t = mau.tokenCua(mau.nhanVien);
        assertEquals("NV03", it.xacThuc("GET", "/api/auth/cau-hinh", bearer(t)).nguoiDung().ma());
    }

    @Test
    void tatDangNhap_moiRequestChayVoiQuanLyDauTien() {
        XacThucInterceptor tat = mau.interceptor(false);
        XacThucInterceptor.KetQua kq = tat.xacThuc("DELETE", "/api/nhan-vien/3", null);
        assertEquals(0, kq.status());
        assertEquals("NV01", kq.nguoiDung().ma());
        assertEquals(NguoiDung.QUAN_LY, kq.nguoiDung().vaiTro());
    }

    @Test
    void layToken_tuHeaderBearer() {
        assertEquals("abc", XacThucInterceptor.layToken("Bearer abc"));
        assertEquals("abc", XacThucInterceptor.layToken("bearer   abc "));
        assertNull(XacThucInterceptor.layToken(null));
        assertNull(XacThucInterceptor.layToken("Token abc"));
    }
}
