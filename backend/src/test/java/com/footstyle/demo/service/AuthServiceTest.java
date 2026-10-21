package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.dto.DangNhapRequest;
import com.footstyle.demo.dto.DoiMatKhauRequest;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.security.GioiHanDangNhap;
import com.footstyle.demo.security.NguoiDung;
import com.footstyle.demo.security.TaiKhoanMau;
import com.footstyle.demo.security.TokenXacThuc;
import com.footstyle.demo.security.XacThuc;
import com.footstyle.demo.security.XacThucInterceptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class AuthServiceTest {

    private final TaiKhoanMau mau = new TaiKhoanMau();
    private final List<String[]> thuDaGui = new ArrayList<>();
    private final MailService mail = new MailService(new MailServiceTest.NoiCap(null), "", "", "", "FootStyle", "") {
        @Override
        public void guiMatKhauTam(String hoTen, String email, String matKhauTam) {
            thuDaGui.add(new String[] {hoTen, email, matKhauTam});
        }
    };
    private final AuthService service = new AuthService(mau.nhanVienRepo(), mau.khachHangRepo(), mau.encoder, mau.token,
            new GioiHanDangNhap(), mail);

    @AfterEach
    void don() {
        XacThuc.xoa();
    }

    private static ApiException loi(Runnable r) {
        return assertThrows(ApiException.class, r::run);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> nd(Map<String, Object> kq) {
        return (Map<String, Object>) kq.get("nguoiDung");
    }

    @Test
    void dangNhapBangEmailSdtHoacMa_khongPhanBietHoaThuongEmailVaMa() {
        for (String tk : new String[] {"long.nh@footstyle.vn", "LONG.NH@FootStyle.vn", "0912000001", "nv01", " NV01 "}) {
            Map<String, Object> kq = service.dangNhapNhanVien(new DangNhapRequest(tk, TaiKhoanMau.MAT_KHAU));
            assertEquals("NV01", nd(kq).get("ma"), tk);
            assertEquals(NguoiDung.QUAN_LY, nd(kq).get("vaiTro"));
            assertEquals("Quản lý", nd(kq).get("tenVaiTro"));
            assertTrue(mau.token.doc((String) kq.get("token")).hopLe());
            assertFalse(nd(kq).containsKey("matKhau"), "không bao giờ trả mật khẩu");
        }
        assertEquals(NguoiDung.NHAN_VIEN, nd(service.dangNhapNhanVien(new DangNhapRequest("bao.lq@footstyle.vn", TaiKhoanMau.MAT_KHAU))).get("vaiTro"));
    }

    @Test
    void thieuThongTin_baoTheoTungO() {
        ApiException e = loi(() -> service.dangNhapNhanVien(new DangNhapRequest("  ", "")));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        assertEquals(List.of("taiKhoan", "matKhau"), List.copyOf(e.getLoiTheoTruong().keySet()));
        assertEquals(HttpStatus.BAD_REQUEST, loi(() -> service.dangNhapNhanVien(null)).getStatus());
    }

    @Test
    void saiMatKhauHoacKhongCoTaiKhoan_cungMotCauBao() {
        ApiException sai = loi(() -> service.dangNhapNhanVien(new DangNhapRequest("long.nh@footstyle.vn", "sai-mat-khau-1")));
        ApiException khongCo = loi(() -> service.dangNhapNhanVien(new DangNhapRequest("khong.co@footstyle.vn", "sai-mat-khau-1")));
        assertEquals(HttpStatus.UNAUTHORIZED, sai.getStatus());
        assertEquals(HttpStatus.UNAUTHORIZED, khongCo.getStatus());
        assertEquals(AuthService.LOI_SAI, sai.getMessage());
        assertEquals(AuthService.LOI_SAI, khongCo.getMessage());
    }

    @Test
    void sai5Lan_khoaTam_duNhapDungCungChuaVaoDuoc() {
        for (int i = 1; i <= 4; i++) {
            ApiException e = loi(() -> service.dangNhapNhanVien(new DangNhapRequest("NV03", "sai")));
            assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
            if (i >= 3) assertTrue(e.getMessage().contains("Còn"), "sắp bị khóa thì báo số lần còn lại");
        }
        ApiException lan5 = loi(() -> service.dangNhapNhanVien(new DangNhapRequest("NV03", "sai")));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, lan5.getStatus());
        ApiException dungMaVanKhoa = loi(() -> service.dangNhapNhanVien(new DangNhapRequest("nv03", TaiKhoanMau.MAT_KHAU)));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, dungMaVanKhoa.getStatus());
        assertTrue(dungMaVanKhoa.getMessage().contains("5 phút"));
        // tài khoản khác không bị ảnh hưởng
        assertEquals("NV01", nd(service.dangNhapNhanVien(new DangNhapRequest("NV01", TaiKhoanMau.MAT_KHAU))).get("ma"));
    }

    @Test
    void taiKhoanNgungHoatDong_chiBaoSauKhiDungMatKhau() {
        assertEquals(HttpStatus.UNAUTHORIZED, loi(() -> service.dangNhapNhanVien(new DangNhapRequest("NV10", "sai"))).getStatus());
        ApiException e = loi(() -> service.dangNhapNhanVien(new DangNhapRequest("NV10", TaiKhoanMau.MAT_KHAU)));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
        assertTrue(e.getMessage().contains("ngừng hoạt động"));
    }

    @Test
    void khachHangDangNhap_tokenLoaiKH() {
        Map<String, Object> kq = service.dangNhapKhachHang(new DangNhapRequest("0901000005", TaiKhoanMau.MAT_KHAU));
        assertEquals("KH05", nd(kq).get("ma"));
        assertEquals(NguoiDung.KHACH_HANG, nd(kq).get("vaiTro"));
        assertEquals("KH", mau.token.doc((String) kq.get("token")).thongTin().loai());
        assertEquals(HttpStatus.UNAUTHORIZED, loi(() -> service.dangNhapKhachHang(new DangNhapRequest("anh.hd@gmail.com", "sai"))).getStatus());
    }

    private void dangNhapLa(com.footstyle.demo.entity.NhanVien n) {
        XacThuc.dat(XacThucInterceptor.tuNhanVien(n));
    }

    @Test
    void doiMatKhau_kiemTraTungO() {
        dangNhapLa(mau.nhanVien);
        ApiException e = loi(() -> service.doiMatKhau(new DoiMatKhauRequest("sai", "abc", "abd")));
        assertEquals("Mật khẩu hiện tại không đúng.", e.getLoiTheoTruong().get("matKhauCu"));
        assertEquals("Mật khẩu từ 8 đến 50 ký tự.", e.getLoiTheoTruong().get("matKhauMoi"));
        assertEquals("Mật khẩu nhập lại không khớp.", e.getLoiTheoTruong().get("nhapLai"));
        e = loi(() -> service.doiMatKhau(new DoiMatKhauRequest(TaiKhoanMau.MAT_KHAU, "chiCoChuThoi", "chiCoChuThoi")));
        assertEquals("Mật khẩu phải có cả chữ và số, không chứa khoảng trắng.", e.getLoiTheoTruong().get("matKhauMoi"));
        e = loi(() -> service.doiMatKhau(new DoiMatKhauRequest(TaiKhoanMau.MAT_KHAU, TaiKhoanMau.MAT_KHAU, TaiKhoanMau.MAT_KHAU)));
        assertEquals("Mật khẩu mới phải khác mật khẩu hiện tại.", e.getLoiTheoTruong().get("matKhauMoi"));
        e = loi(() -> service.doiMatKhau(new DoiMatKhauRequest("", "", "")));
        assertEquals(3, e.getLoiTheoTruong().size());
    }

    @Test
    void doiMatKhauThanhCong_tokenCuHetHieuLuc_matKhauMoiDangNhapDuoc() {
        String tokenCu = mau.tokenCua(mau.nhanVien);
        dangNhapLa(mau.nhanVien);
        Map<String, Object> kq = service.doiMatKhau(new DoiMatKhauRequest(TaiKhoanMau.MAT_KHAU, "MatKhauMoi2026", "MatKhauMoi2026"));
        String tokenMoi = (String) kq.get("token");
        assertNotEquals(tokenCu, tokenMoi);
        XacThucInterceptor it = mau.interceptor(true);
        assertEquals(401, it.xacThuc("GET", "/api/hoa-don", "Bearer " + tokenCu).status());
        assertEquals(0, it.xacThuc("GET", "/api/hoa-don", "Bearer " + tokenMoi).status());
        assertTrue(mau.encoder.matches("MatKhauMoi2026", mau.nhanVien.getMatKhau()), "lưu dạng băm");
        assertNotEquals("MatKhauMoi2026", mau.nhanVien.getMatKhau());
        assertEquals("NV03", nd(service.dangNhapNhanVien(new DangNhapRequest("NV03", "MatKhauMoi2026"))).get("ma"));
    }

    @Test
    void doiMatKhau_chuaDangNhap_401() {
        assertEquals(HttpStatus.UNAUTHORIZED, loi(() -> service.doiMatKhau(new DoiMatKhauRequest("a", "b", "c"))).getStatus());
    }

    @Test
    void quenMatKhau_emailDung_doiMatKhauTamVaGuiMail_emailSaiCungCauBaoNhuNhau() {
        String cu = mau.nhanVien.getMatKhau();
        Map<String, Object> a = service.quenMatKhau("BAO.LQ@footstyle.vn");
        assertNotEquals(cu, mau.nhanVien.getMatKhau());
        assertEquals(1, thuDaGui.size());
        assertEquals("bao.lq@footstyle.vn", thuDaGui.get(0)[1]);
        assertTrue(mau.encoder.matches(thuDaGui.get(0)[2], mau.nhanVien.getMatKhau()), "mật khẩu trong thư đăng nhập được");
        Map<String, Object> b = service.quenMatKhau("khong.co@footstyle.vn");
        assertEquals(a.get("message"), b.get("message"), "không lộ email nào có tài khoản");
        String nghi = mau.nghiViec.getMatKhau();
        service.quenMatKhau("huong.dm@footstyle.vn");
        assertEquals(nghi, mau.nghiViec.getMatKhau(), "tài khoản ngừng hoạt động không được cấp lại");
        assertEquals(1, thuDaGui.size());
        assertEquals(HttpStatus.BAD_REQUEST, loi(() -> service.quenMatKhau("khong-phai-email")).getStatus());
        assertNull(XacThuc.hienTai().orElse(null));
    }

    @Test
    void tokenSauDangNhap_hetHan12Gio() {
        Map<String, Object> kq = service.dangNhapNhanVien(new DangNhapRequest("NV03", TaiKhoanMau.MAT_KHAU));
        TokenXacThuc.ThongTin tt = mau.token.doc((String) kq.get("token")).thongTin();
        long conLai = tt.hetHan() - java.time.Instant.now().getEpochSecond();
        assertTrue(conLai > 11 * 3600 && conLai <= 12 * 3600);
    }
}
