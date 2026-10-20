package com.footstyle.demo.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.entity.DiaChiKhachHang;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.service.QuyTacDuLieu;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** Kiểm tra dữ liệu địa chỉ khách hàng trước khi lưu (chặn cả khi gọi thẳng API, không qua form). */
class DiaChiKhachHangControllerTest {

    private static DiaChiKhachHang dc(String ten, String sdt, String tinh, String phuong, String chiTiet) {
        DiaChiKhachHang d = new DiaChiKhachHang();
        d.setTenNguoiNhan(ten);
        d.setSdtNguoiNhan(sdt);
        d.setTinhThanhPho(tinh);
        d.setPhuongXa(phuong);
        d.setDiaChiCuThe(chiTiet);
        return d;
    }

    private static String loi(DiaChiKhachHang d) {
        return assertThrows(ApiException.class, () -> DiaChiKhachHangController.chuanHoa(d)).getMessage();
    }


    @Test
    void hopLe_boKhoangTrangThua() {
        DiaChiKhachHang d = dc("  Nguyễn   An ", " 0912345678 ", "Thành phố Hà Nội", "Phường Hà Đông", "  Số 1   Trần Phú ");
        d.setQuanHuyen("   ");
        DiaChiKhachHangController.chuanHoa(d);
        assertEquals("Nguyễn An", d.getTenNguoiNhan());
        assertEquals("0912345678", d.getSdtNguoiNhan());
        assertEquals("Số 1 Trần Phú", d.getDiaChiCuThe());
        assertNull(d.getQuanHuyen(), "quận/huyện trống (địa chỉ sau sáp nhập) lưu null");
    }

    @Test
    void chiGoDauCach_coiNhuChuaNhap() {
        assertTrue(loi(dc("   ", "0912345678", "Hà Nội", "Hà Đông", "Số 1")).contains("tên người nhận"));
        assertTrue(loi(dc("An", "0912345678", " ", "Hà Đông", "Số 1")).contains("Tỉnh"));
        assertTrue(loi(dc("An", "0912345678", "Hà Nội", "", "Số 1")).contains("Phường"));
        assertTrue(loi(dc("An", "0912345678", "Hà Nội", "Hà Đông", "  ")).contains("địa chỉ cụ thể"));
    }

    @Test
    void sdtSaiDinhDang_baoLoi() {
        // Chỉ nhận đúng 10 chữ số: 9 số, 11 số, 12 số, có chữ, có +84 đều sai
        for (String sdt : new String[] {null, "", "091234567", "09123456789", "091234567890", "09123a5678", "+84912345678"}) {
            assertTrue(loi(dc("An", sdt, "Hà Nội", "Hà Đông", "Số 1")).toLowerCase().contains("số điện thoại"), String.valueOf(sdt));
        }
    }

    @Test
    void sdtPhaiDungDauSoDiDong() {
        // không bắt đầu bằng 0 bị chặn
        for (String sai : new String[] {"1112345678", "1212345678", "9412345678", "8612345678", "1912345678"}) {
            assertEquals(QuyTacDuLieu.LOI_SDT, loi(dc("An", sai, "Hà Nội", "Hà Đông", "Số 1")), sai);
        }
        for (String dung : new String[] {"0112345678", "0212345678", "0312345678", "0512345678", "0712345678", "0812345678", "0912345678"}) {
            DiaChiKhachHang d = dc("An", dung, "Hà Nội", "Hà Đông", "Số 1");
            DiaChiKhachHangController.chuanHoa(d);
            assertEquals(dung, d.getSdtNguoiNhan());
        }
    }

    @Test
    void tenNguoiNhanChiGomChuCai() {
        for (String sai : new String[] {"An 123", "Mẹ@nhà", "12345", "A"}) {
            ApiException e = assertThrows(ApiException.class, () -> DiaChiKhachHangController.chuanHoa(dc(sai, "0912345678", "Hà Nội", "Hà Đông", "Số 1")));
            assertTrue(e.getLoiTheoTruong().containsKey("tenNguoiNhan"), sai);
        }
    }

    @Test
    void loiBaoTheoTungOCungLuc() {
        ApiException e = assertThrows(ApiException.class, () -> DiaChiKhachHangController.chuanHoa(dc("", "1212345678", "", " ", "")));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        assertEquals(List.of("tenNguoiNhan", "sdtNguoiNhan", "tinhThanhPho", "phuongXa", "diaChiCuThe"), List.copyOf(e.getLoiTheoTruong().keySet()));
        assertEquals("Vui lòng nhập tên người nhận.", e.getLoiTheoTruong().get("tenNguoiNhan"));
        assertEquals(QuyTacDuLieu.LOI_SDT, e.getLoiTheoTruong().get("sdtNguoiNhan"));
        assertEquals("Vui lòng chọn Tỉnh/Thành phố.", e.getLoiTheoTruong().get("tinhThanhPho"));
        assertEquals("Vui lòng chọn Phường/Xã.", e.getLoiTheoTruong().get("phuongXa"));
        assertEquals("Vui lòng nhập địa chỉ cụ thể (số nhà, tên đường...).", e.getLoiTheoTruong().get("diaChiCuThe"));
        assertEquals("Vui lòng nhập tên người nhận.", e.getMessage()); // câu chung = lỗi của ô đầu tiên
    }

    @Test
    void quaDai_baoLoi() {
        assertTrue(loi(dc("An", "0912345678", "Hà Nội", "Hà Đông", "x".repeat(256))).contains("255"));
        assertTrue(loi(dc("x".repeat(101), "0912345678", "Hà Nội", "Hà Đông", "Số 1")).contains("100"));
        assertTrue(loi(null).contains("Thiếu"));
    }

    // ---------- địa chỉ cụ thể phải có chữ hoặc số ----------
    @Test
    void diaChiCuTheChiToanKyTuDacBiet_baoLoiTaiOdiaChiCuThe() {
        for (String sai : new String[] {"---", "...", "@#$%", "- . -"}) {
            ApiException e = assertThrows(ApiException.class,
                    () -> DiaChiKhachHangController.chuanHoa(dc("An", "0912345678", "Hà Nội", "Hà Đông", sai)));
            assertEquals(DiaChiKhachHangController.LOI_DIA_CHI_CHI_TIET_CHU_SO, e.getLoiTheoTruong().get("diaChiCuThe"), sai);
        }
        for (String dung : new String[] {"12", "Ngõ 5", "Số 1/2 Trần Phú", "Thôn Đoài"}) {
            assertDoesNotThrow(() -> DiaChiKhachHangController.chuanHoa(dc("An", "0912345678", "Hà Nội", "Hà Đông", dung)), dung);
        }
    }

    // ---------- giới hạn số địa chỉ ----------
    @Test
    void toiDaMuoiDiaChi_themDiaChiThuMuoiMotBiChan() {
        assertEquals(10, DiaChiKhachHangController.SO_DIA_CHI_MAX);
        assertDoesNotThrow(() -> DiaChiKhachHangController.kiemTraSoLuong(0));
        assertDoesNotThrow(() -> DiaChiKhachHangController.kiemTraSoLuong(9), "đang có 9 thì thêm được địa chỉ thứ 10");
        ApiException e = assertThrows(ApiException.class, () -> DiaChiKhachHangController.kiemTraSoLuong(10));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        assertTrue(e.getMessage().contains("10"));
    }

    // ---------- địa chỉ trùng ----------
    private static DiaChiKhachHang coId(Integer id, DiaChiKhachHang d) {
        d.setId(id);
        return d;
    }

    @Test
    void diaChiTrungTinhPhuongChiTietVaSdt_baoTrung409_khongPhanBietHoaThuong() {
        List<DiaChiKhachHang> hienCo = List.of(coId(1, dc("An", "0912345678", "Thành phố Hà Nội", "Phường Hà Đông", "Số 1 Trần Phú")));
        DiaChiKhachHang trung = dc("Bình", "0912345678", "thành phố hà nội", "PHƯỜNG HÀ ĐÔNG", "số 1   trần phú");
        ApiException e = assertThrows(ApiException.class, () -> DiaChiKhachHangController.kiemTraTrung(trung, hienCo, null));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        assertEquals(DiaChiKhachHangController.LOI_TRUNG, e.getMessage());
    }

    @Test
    void khacMotTrongBonTruong_khongPhaiDiaChiTrung() {
        List<DiaChiKhachHang> hienCo = List.of(coId(1, dc("An", "0912345678", "Thành phố Hà Nội", "Phường Hà Đông", "Số 1 Trần Phú")));
        // khác SĐT (cùng nhà nhưng người nhận khác số), khác số nhà, khác phường, khác tỉnh
        assertDoesNotThrow(() -> DiaChiKhachHangController.kiemTraTrung(dc("An", "0912345679", "Thành phố Hà Nội", "Phường Hà Đông", "Số 1 Trần Phú"), hienCo, null));
        assertDoesNotThrow(() -> DiaChiKhachHangController.kiemTraTrung(dc("An", "0912345678", "Thành phố Hà Nội", "Phường Hà Đông", "Số 2 Trần Phú"), hienCo, null));
        assertDoesNotThrow(() -> DiaChiKhachHangController.kiemTraTrung(dc("An", "0912345678", "Thành phố Hà Nội", "Phường Cầu Giấy", "Số 1 Trần Phú"), hienCo, null));
        assertDoesNotThrow(() -> DiaChiKhachHangController.kiemTraTrung(dc("An", "0912345678", "Thành phố Huế", "Phường Hà Đông", "Số 1 Trần Phú"), hienCo, null));
    }

    @Test
    void suaChinhDiaChiDo_khongTinhLaTrungVoiChinNo_nhungTrungDiaChiKhacThiBao() {
        DiaChiKhachHang a = coId(1, dc("An", "0912345678", "Thành phố Hà Nội", "Phường Hà Đông", "Số 1 Trần Phú"));
        DiaChiKhachHang b = coId(2, dc("An", "0912345678", "Thành phố Hà Nội", "Phường Hà Đông", "Số 2 Trần Phú"));
        List<DiaChiKhachHang> hienCo = List.of(a, b);
        // Sửa địa chỉ 1 mà giữ nguyên nội dung: bỏ qua chính nó
        assertDoesNotThrow(() -> DiaChiKhachHangController.kiemTraTrung(
                dc("An", "0912345678", "Thành phố Hà Nội", "Phường Hà Đông", "Số 1 Trần Phú"), hienCo, 1));
        // Sửa địa chỉ 1 thành giống hệt địa chỉ 2: trùng
        assertEquals(HttpStatus.CONFLICT, assertThrows(ApiException.class, () -> DiaChiKhachHangController.kiemTraTrung(
                dc("An", "0912345678", "Thành phố Hà Nội", "Phường Hà Đông", "Số 2 Trần Phú"), hienCo, 1)).getStatus());
    }
}
