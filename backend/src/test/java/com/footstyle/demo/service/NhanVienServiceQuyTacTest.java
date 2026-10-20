package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.dto.DiaChiNhanVienDto;
import com.footstyle.demo.dto.NhanVienRequest;
import com.footstyle.demo.dto.NhanVienResponse;
import com.footstyle.demo.dto.VaiTroResponse;
import com.footstyle.demo.exception.ApiException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Các luật mới của form Nhân viên theo lời cô: lỗi báo TỪNG Ô cùng lúc, tuổi từ đủ 18 (BLLĐ 2019 Điều 143),
 * họ tên không có chữ số, mật khẩu có chữ + số, chỉ có hai quyền (Quản lý, Nhân viên), form SỬA cũng validate như THÊM.
 */
class NhanVienServiceQuyTacTest {

    private final KhoNhanVienGia kho = new KhoNhanVienGia();
    private final NhanVienService service = kho.service;

    private static List<DiaChiNhanVienDto> diaChi() {
        return List.of(new DiaChiNhanVienDto(null, "Thành phố Hà Nội", "Phường Hà Đông", "Số 15 ngõ 120 Trần Phú", true));
    }

    private static NhanVienRequest hopLe() {
        return new NhanVienRequest("Lê Quốc Tuấn", "Nam", LocalDate.of(1999, 5, 20), "0987000001", "tuan@footstyle.vn", null,
                LocalDate.of(2024, 1, 1), 2, "Footstyle123", null, diaChi());
    }

    /** Sao chép request hợp lệ rồi thay MỘT trường. */
    private static NhanVienRequest voi(String truong, Object giaTri) {
        NhanVienRequest r = hopLe();
        return new NhanVienRequest(
                "hoTen".equals(truong) ? (String) giaTri : r.hoTen(),
                "gioiTinh".equals(truong) ? (String) giaTri : r.gioiTinh(),
                "ngaySinh".equals(truong) ? (LocalDate) giaTri : r.ngaySinh(),
                "soDienThoai".equals(truong) ? (String) giaTri : r.soDienThoai(),
                "email".equals(truong) ? (String) giaTri : r.email(),
                r.diaChi(),
                "ngayVaoLam".equals(truong) ? (LocalDate) giaTri : r.ngayVaoLam(),
                "idVaiTro".equals(truong) ? (Integer) giaTri : r.idVaiTro(),
                "matKhau".equals(truong) ? (String) giaTri : r.matKhau(),
                "canCuoc".equals(truong) ? (String) giaTri : r.canCuoc(),
                r.diaChis());
    }

    private ApiException loi(NhanVienRequest r) {
        return assertThrows(ApiException.class, () -> service.them(r));
    }

    private String loiO(NhanVienRequest r, String truong) {
        ApiException e = loi(r);
        assertTrue(e.getLoiTheoTruong().containsKey(truong), "phải có lỗi ở ô " + truong + " nhưng chỉ có " + e.getLoiTheoTruong());
        return e.getLoiTheoTruong().get(truong);
    }

    @Test
    void formTrongBaoLoiMoiOCungLuc() {
        NhanVienRequest trong = new NhanVienRequest(null, null, null, null, null, null, null, null, null, null, null);
        ApiException e = loi(trong);
        Map<String, String> theoO = e.getLoiTheoTruong();
        assertEquals("Nhập email.", theoO.get("email"));
        assertEquals("Nhập mật khẩu cho nhân viên mới.", theoO.get("matKhau"));
        assertEquals("Nhập họ tên.", theoO.get("hoTen"));
        assertEquals("Nhập số điện thoại.", theoO.get("soDienThoai"));
        assertEquals("Chọn giới tính.", theoO.get("gioiTinh"));
        assertEquals("Nhập ngày sinh.", theoO.get("ngaySinh"));
        assertEquals("Nhập ít nhất một địa chỉ.", theoO.get("diaChis"));
        assertEquals("Chọn vai trò.", theoO.get("idVaiTro"));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        assertEquals(2, kho.nhanViens.size(), "không lưu gì");
    }

    @Test
    void ngaySinhBatBuocVaPhaiDu18Tuoi() {
        assertEquals("Nhập ngày sinh.", loiO(voi("ngaySinh", null), "ngaySinh"));
        LocalDate hom = LocalDate.now();
        assertEquals("Nhân viên phải từ đủ 18 tuổi.", loiO(voi("ngaySinh", hom.minusYears(18).plusDays(1)), "ngaySinh"));
        assertEquals("Nhân viên phải từ đủ 18 tuổi.", loiO(voi("ngaySinh", hom.minusYears(16)), "ngaySinh"));
        assertEquals("Ngày sinh không được ở tương lai.", loiO(voi("ngaySinh", hom.plusDays(1)), "ngaySinh"));
        assertTrue(loiO(voi("ngaySinh", hom.minusYears(101)), "ngaySinh").contains("quá 100 tuổi"));
        // Đúng 18 tuổi hôm nay (vào làm cùng ngày) là hợp lệ
        NhanVienRequest r18 = new NhanVienRequest("Lê Quốc Tuấn", "Nam", hom.minusYears(18), "0987000001", "tuan@footstyle.vn", null, hom, 2,
                "Footstyle123", null, diaChi());
        assertEquals("LQT01", service.them(r18).ma());
    }

    @Test
    void hoTenKhongCoChuSoKyTuDacBiet() {
        for (String ten : new String[] {"Lê Quốc 99", "123", "Lê@Tuấn", "_An"}) {
            assertEquals("Họ tên chỉ gồm chữ cái và khoảng trắng, không có chữ số hay ký tự đặc biệt.", loiO(voi("hoTen", ten), "hoTen"), ten);
        }
        assertEquals("Nhập họ tên.", loiO(voi("hoTen", "   "), "hoTen"));
        assertEquals("A", service.them(voi("hoTen", "A")).hoTen(), "1 ký tự vẫn hợp lệ");
    }

    @Test
    void soDienThoaiDauSoVaKhongTrung() {
        assertEquals(QuyTacDuLieu.LOI_SDT, loiO(voi("soDienThoai", "1212345678"), "soDienThoai"));
        assertEquals(QuyTacDuLieu.LOI_SDT, loiO(voi("soDienThoai", "123"), "soDienThoai"));
        assertEquals("0212345678", service.them(voi("soDienThoai", "0212345678")).soDienThoai(), "bắt đầu bằng 0 và 10 số là hợp lệ");
        ApiException trung = loi(voi("soDienThoai", "0912000002"));
        assertEquals(HttpStatus.CONFLICT, trung.getStatus());
        assertEquals("Số điện thoại đã được sử dụng.", trung.getLoiTheoTruong().get("soDienThoai"));
    }

    @Test
    void matKhauPhaiCoChuVaSoKhongKhoangTrang() {
        assertEquals("Mật khẩu từ 8 đến 50 ký tự.", loiO(voi("matKhau", "Ab1"), "matKhau"));
        assertEquals("Mật khẩu từ 8 đến 50 ký tự.", loiO(voi("matKhau", "Ab1" + "x".repeat(50)), "matKhau"));
        for (String mk : new String[] {"chiCoChuCai", "123456789", "co khoang trang 1"}) {
            assertEquals("Mật khẩu phải có cả chữ và số, không chứa khoảng trắng.", loiO(voi("matKhau", mk), "matKhau"), mk);
        }
        assertEquals("LQT01", service.them(voi("matKhau", "Abcdef12")).ma(), "8 ký tự, có chữ và số là đủ");
    }

    @Test
    void ngayVaoLamKhongOTuongLai_khongNhapThiLayHomNay() {
        assertEquals("Ngày vào làm không được ở tương lai.", loiO(voi("ngayVaoLam", LocalDate.now().plusDays(1)), "ngayVaoLam"));
        NhanVienResponse r = service.them(voi("ngayVaoLam", null));
        assertFalse(r.ngayVaoLam().isAfter(LocalDate.now()));
        assertFalse(r.ngayVaoLam().isBefore(LocalDate.now().minusDays(1)));
    }

    @Test
    void emailSaiDinhDangVaTrung() {
        assertTrue(loiO(voi("email", "khong-phai-email"), "email").startsWith("Email chưa đúng định dạng"));
        assertEquals("Nhập email.", loiO(voi("email", "  "), "email"));
        ApiException trung = loi(voi("email", "LONG@footstyle.vn"));
        assertEquals(HttpStatus.CONFLICT, trung.getStatus());
        assertEquals("Email đã được sử dụng.", trung.getLoiTheoTruong().get("email"));
    }

    @Test
    void chiCoHaiVaiTro_vaTroKhacBiTuChoi() {
        List<VaiTroResponse> ds = service.getVaiTro();
        assertEquals(List.of("Quản lý", "Nhân viên"), ds.stream().map(VaiTroResponse::ten).toList(),
                "'Kế toán' đang dùng trong CSDL nhưng không phải quyền hợp lệ nên không hiện trong danh sách");
        assertEquals("Chỉ có hai vai trò: Quản lý và Nhân viên.", loiO(voi("idVaiTro", 3), "idVaiTro"));
        assertEquals("Vai trò này đang ngưng sử dụng.", loiO(voi("idVaiTro", 4), "idVaiTro"));
        assertEquals("Vai trò không tồn tại.", loiO(voi("idVaiTro", 99), "idVaiTro"));
        assertEquals("Chọn vai trò.", loiO(voi("idVaiTro", null), "idVaiTro"));
        assertEquals("Quản lý", service.them(voi("idVaiTro", 1)).vaiTro());
    }

    @Test
    void loiDauTienLaMessage_loiTrungThi409_conLaiThi400() {
        // Họ tên sai (400) đứng trước email trùng (409): message là lỗi họ tên? Thứ tự kiểm tra: email -> ... -> họ tên
        NhanVienRequest vua = new NhanVienRequest("An 1", "Nam", LocalDate.of(1999, 5, 20), "0987000001", "long@footstyle.vn", null,
                LocalDate.of(2024, 1, 1), 2, "Footstyle123", null, diaChi());
        ApiException e = loi(vua);
        assertEquals(HttpStatus.CONFLICT, e.getStatus(), "email là ô được kiểm tra đầu tiên và bị trùng");
        assertEquals("Email đã được sử dụng.", e.getMessage());
        assertEquals(2, e.getLoiTheoTruong().size());
        assertTrue(e.getLoiTheoTruong().containsKey("hoTen"));
    }

    // ---------- form SỬA cũng validate như THÊM ----------

    private NhanVienResponse taoMau() {
        return service.them(hopLe());
    }

    private NhanVienRequest suaVoi(NhanVienResponse goc, String hoTen, LocalDate ngaySinh, String sdt, String gioiTinh) {
        return new NhanVienRequest(hoTen, gioiTinh, ngaySinh, sdt, null, null, null, 2, null, null, goc.diaChis());
    }

    @Test
    void suaCungBaoLoiTungOGiongHetThem() {
        NhanVienResponse goc = taoMau();
        NhanVienRequest sai = suaVoi(goc, "Lê 99", LocalDate.now().minusYears(10), "1212345678", null);
        ApiException e = assertThrows(ApiException.class, () -> service.sua(goc.id(), sai));
        assertEquals(Map.of("hoTen", QuyTacDuLieu.LOI_HO_TEN_KY_TU, "soDienThoai", QuyTacDuLieu.LOI_SDT, "gioiTinh", "Chọn giới tính.",
                "ngaySinh", "Nhân viên phải từ đủ 18 tuổi."), e.getLoiTheoTruong());
        // Không có lỗi mật khẩu / email / ngày vào làm vì khi sửa các ô này không đổi
        assertFalse(e.getLoiTheoTruong().containsKey("matKhau"));
        assertFalse(e.getLoiTheoTruong().containsKey("email"));
        assertFalse(e.getLoiTheoTruong().containsKey("ngayVaoLam"));
        // Dữ liệu cũ còn nguyên
        assertEquals("Lê Quốc Tuấn", service.getById(goc.id()).hoTen());
    }

    @Test
    void suaChanNgaySinhKhongDu18Tuoi_vaNgaySinhTrong() {
        NhanVienResponse goc = taoMau();
        ApiException e = assertThrows(ApiException.class,
                () -> service.sua(goc.id(), suaVoi(goc, "Lê Quốc Tuấn", null, "0987000001", "Nam")));
        assertEquals("Nhập ngày sinh.", e.getLoiTheoTruong().get("ngaySinh"));
        // Sửa ngày sinh làm nhân viên "chưa đủ 18 tuổi khi vào làm" (vào làm 01/01/2024 -> sinh sau 01/01/2006 là sai)
        ApiException e2 = assertThrows(ApiException.class,
                () -> service.sua(goc.id(), suaVoi(goc, "Lê Quốc Tuấn", LocalDate.of(2007, 1, 1), "0987000001", "Nam")));
        assertTrue(e2.getLoiTheoTruong().get("ngaySinh").contains("đủ 18 tuổi vào ngày vào làm (01/01/2024)"), e2.getLoiTheoTruong().toString());
    }

    @Test
    void suaDuocKhiHopLe_vaGiuNguyenEmailMatKhauNgayVaoLam() {
        NhanVienResponse goc = taoMau();
        String matKhauCu = kho.nhanViens.get(kho.nhanViens.size() - 1).getMatKhau();
        NhanVienResponse sau = service.sua(goc.id(), suaVoi(goc, "Lê Quốc Tuấn B", LocalDate.of(1998, 1, 2), "0987000009", "Nam"));
        assertEquals("Lê Quốc Tuấn B", sau.hoTen());
        assertEquals("0987000009", sau.soDienThoai());
        assertEquals("tuan@footstyle.vn", sau.email());
        assertEquals(LocalDate.of(2024, 1, 1), sau.ngayVaoLam());
        assertEquals(matKhauCu, kho.nhanViens.get(kho.nhanViens.size() - 1).getMatKhau());
        assertNull(sau.canCuoc());
    }

    @Test
    void themLo_maKhongTrungDuCungChuCaiDauVaLuuDiaChiChinh() {
        NhanVienService.DuLieuNhanVien a = du("Lê Quốc Tuấn", "a@footstyle.vn", "0987000001");
        NhanVienService.DuLieuNhanVien b = du("Lê Quang Tú", "b@footstyle.vn", "0987000002");
        List<NhanVienResponse> kq = service.themLo(List.of(a, b));
        assertEquals(List.of("LQT01", "LQT02"), kq.stream().map(NhanVienResponse::ma).toList());
        assertEquals(4, kho.nhanViens.size());
        assertTrue(kq.stream().allMatch(x -> x.hoatDong() && x.diaChis().size() == 1 && x.diaChis().get(0).macDinh()));
        assertTrue(kho.bam.matches("Abcdef1234", kho.theoEmail("a@footstyle.vn").getMatKhau()));
    }

    private NhanVienService.DuLieuNhanVien du(String hoTen, String email, String sdt) {
        com.footstyle.demo.exception.LoiForm lf = new com.footstyle.demo.exception.LoiForm();
        NhanVienService.DuLieuNhanVien d = service.kiemTra(new NhanVienRequest(hoTen, "Nam", LocalDate.of(1999, 5, 20), sdt, email, null,
                LocalDate.of(2024, 1, 1), 2, "Abcdef1234", null, diaChi()), true, null, lf);
        assertFalse(lf.coLoi(), lf.theoTruong().toString());
        return d;
    }
}
