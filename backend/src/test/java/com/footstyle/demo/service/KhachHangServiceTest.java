package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.KhoGia;
import com.footstyle.demo.dto.KhachHangRequest;
import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.KhachHangRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * KhachHangService với kho giả (so email / SĐT KHÔNG phân biệt hoa thường giống SQL Server).
 * Các "case khó" cô yêu cầu: đầu số SĐT, trùng email / SĐT (cả khi sửa), tuổi tối thiểu 15, lỗi báo theo TỪNG Ô.
 */
class KhachHangServiceTest {

    private final List<KhachHang> khachs = new ArrayList<>();
    /** true: giả lập khách đã có hóa đơn / phiếu giảm giá, khóa ngoại làm lệnh xóa thất bại khi flush. */
    private boolean khoaNgoaiChan = false;
    private final KhachHangService service;

    KhachHangServiceTest() {
        KhachHangRepository repo = KhoGia.tao(KhachHangRepository.class, (ten, a) -> switch (ten) {
            case "existsByEmailIgnoreCaseAndIdNot" -> khachs.stream().anyMatch(k -> k.getEmail() != null && k.getEmail().equalsIgnoreCase((String) a[0]) && !k.getId().equals(a[1]));
            case "existsBySdtAndIdNot" -> khachs.stream().anyMatch(k -> a[0].equals(k.getSdt()) && !k.getId().equals(a[1]));
            case "existsByMaKhachHang" -> khachs.stream().anyMatch(k -> k.getMaKhachHang().equalsIgnoreCase((String) a[0]));
            case "count" -> (long) khachs.size();
            case "delete" -> {
                khachs.remove((KhachHang) a[0]);
                yield null;
            }
            case "flush" -> {
                if (khoaNgoaiChan) throw new DataIntegrityViolationException("FK_hoa_don_khach_hang");
                yield null;
            }
            case "findById" -> khachs.stream().filter(k -> k.getId().equals(a[0])).findFirst();
            case "save" -> {
                KhachHang k = (KhachHang) a[0];
                if (k.getId() == null) {
                    k.setId(khachs.size() + 1);
                    khachs.add(k);
                }
                yield k;
            }
            default -> KhoGia.KHONG_CO;
        });
        service = new KhachHangService(repo, new BCryptPasswordEncoder());
    }

    private static KhachHangRequest req(String hoTen, String sdt, String email) {
        KhachHangRequest r = new KhachHangRequest();
        r.setHoTen(hoTen);
        r.setSdt(sdt);
        r.setEmail(email);
        return r;
    }

    private static ApiException loi(Runnable r) {
        return assertThrows(ApiException.class, r::run);
    }

    private static Map<String, String> theoO(ApiException e) {
        assertNotNull(e.getLoiTheoTruong(), "lỗi phải gắn theo từng ô");
        return e.getLoiTheoTruong();
    }

    private KhachHang mau() {
        return service.them(req("Nguyễn Văn An", "0912345678", "an@mail.vn"));
    }

    // ---------- email ----------
    @Test
    void suaChiDoiHoaThuongCuaEmail_khongBaoTrungVoiChinhMinh() {
        KhachHang k = service.them(req("Nguyễn An", "0912345678", "An@Mail.vn"));
        KhachHang sau = service.sua(k.getId(), req("Nguyễn An", "0912345678", "an@mail.vn"));
        assertEquals("an@mail.vn", sau.getEmail());
    }

    @Test
    void emailTrungKhachKhacDuKhacHoaThuong_baoTrungTaiOEmail() {
        service.them(req("Nguyễn An", "0912345678", "an@mail.vn"));
        KhachHang b = service.them(req("Trần Bình", "0912345679", "binh@mail.vn"));
        ApiException them = loi(() -> service.them(req("Lê Cường", "0912345670", "AN@MAIL.VN")));
        assertEquals(HttpStatus.CONFLICT, them.getStatus());
        assertEquals("Email đã tồn tại.", theoO(them).get("email"));
        assertEquals(HttpStatus.CONFLICT, loi(() -> service.sua(b.getId(), req("Trần Bình", "0912345679", "An@mail.vn"))).getStatus());
        assertEquals(2, khachs.size());
    }

    @Test
    void emailSaiDinhDang_baoLoiTaiOEmail() {
        for (String sai : List.of("an", "an@", "an@mail", "an @mail.vn", "@mail.vn", "an@mail.v")) {
            ApiException e = loi(() -> service.them(req("Nguyễn An", "0912345678", sai)));
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatus(), sai);
            assertTrue(theoO(e).get("email").contains("Email chưa đúng định dạng"), sai);
        }
        assertTrue(khachs.isEmpty());
    }

    @Test
    void emailBatBuocVaDuocLuuDangChuThuong() {
        assertEquals("Vui lòng nhập email.", theoO(loi(() -> service.them(req("Nguyễn An", "0912345678", "  ")))).get("email"));
        assertEquals("an@mail.vn", service.them(req("Nguyễn An", "0912345678", "  AN@Mail.VN ")).getEmail());
    }

    // ---------- số điện thoại ----------
    @Test
    void sdtChuaNhapBaoThieu_nhapHopLeDuocLuu() {
        assertEquals("Vui lòng nhập số điện thoại.", theoO(loi(() -> service.them(req("Nguyễn An", "", "b@mail.vn")))).get("sdt"));
        assertEquals(QuyTacDuLieu.LOI_SDT, theoO(loi(() -> service.them(req("Nguyễn An", "096791622", "an1@mail.vn")))).get("sdt"));
        assertEquals(QuyTacDuLieu.LOI_SDT, theoO(loi(() -> service.them(req("Nguyễn An", "1234567890", "an1@mail.vn")))).get("sdt"));
        assertEquals("0123456789", service.them(req("Nguyễn An", "0123456789", "an1@mail.vn")).getSdt());
        assertEquals("0912345678", service.them(req("Nguyễn An", " 0912345678 ", "an2@mail.vn")).getSdt());
    }

    @Test
    void sdtTrungBaoTrungTaiOSdt_suaChinhSdtCuaMinhThiKhongSao() {
        KhachHang a = mau();
        ApiException them = loi(() -> service.them(req("Trần Bình", "0912345678", "binh@mail.vn")));
        assertEquals(HttpStatus.CONFLICT, them.getStatus());
        assertEquals("Số điện thoại đã tồn tại.", theoO(them).get("sdt"));
        KhachHang b = service.them(req("Trần Bình", "0912345679", "binh@mail.vn"));
        assertEquals(HttpStatus.CONFLICT, loi(() -> service.sua(b.getId(), req("Trần Bình", "0912345678", "binh@mail.vn"))).getStatus());
        // giữ nguyên SĐT của chính mình khi sửa tên
        assertEquals("Nguyễn Văn Ân", service.sua(a.getId(), req("Nguyễn Văn Ân", "0912345678", "an@mail.vn")).getHoTen());
    }

    // ---------- họ tên ----------
    @Test
    void hoTenChiGomChuCai_khongChuSoKyTuDacBiet() {
        for (String sai : List.of("An 123", "Nguyễn@An", "12345", "An_Văn", "<b>An</b>", "A", "   ")) {
            ApiException e = loi(() -> service.them(req(sai, "0912345678", "an@mail.vn")));
            assertTrue(theoO(e).containsKey("hoTen"), sai);
        }
        assertEquals(QuyTacDuLieu.LOI_HO_TEN_KY_TU, theoO(loi(() -> service.them(req("An 123", "0912345678", "an@mail.vn")))).get("hoTen"));
        assertEquals("Vui lòng nhập họ tên khách hàng.", theoO(loi(() -> service.them(req("   ", "0912345678", "an@mail.vn")))).get("hoTen"));
        assertEquals("Họ tên từ 2 đến 100 ký tự.", theoO(loi(() -> service.them(req("A".repeat(101), "0912345678", "an@mail.vn")))).get("hoTen"));
        assertEquals("Nguyễn Thị Ánh Tuyết", service.them(req("  Nguyễn   Thị Ánh   Tuyết ", "0912345678", "an@mail.vn")).getHoTen());
        assertEquals("Lê O'Brien-Smith Jr.", service.them(req("Lê O'Brien-Smith Jr.", "0912345679", "b@mail.vn")).getHoTen());
    }

    // ---------- tuổi tối thiểu 15 ----------
    @Test
    void khachDuoi15TuoiBiChanTaiONgaySinh_tronDu15TuoiThiDuoc() {
        LocalDate homNay = LocalDate.now();
        KhachHangRequest du15 = req("Nguyễn An", "0912345678", "an@mail.vn");
        du15.setNgaySinh(homNay.minusYears(15)); // sinh nhật lần thứ 15 đúng hôm nay
        assertEquals(homNay.minusYears(15), service.them(du15).getNgaySinh());

        KhachHangRequest thieuMotNgay = req("Trần Bình", "0912345679", "binh@mail.vn");
        thieuMotNgay.setNgaySinh(homNay.minusYears(15).plusDays(1)); // mai mới đủ 15 tuổi
        ApiException e = loi(() -> service.them(thieuMotNgay));
        assertEquals("Khách hàng phải từ đủ 15 tuổi.", theoO(e).get("ngaySinh"));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());

        KhachHangRequest treCon = req("Lê Cường", "0912345670", "cuong@mail.vn");
        treCon.setNgaySinh(homNay.minusYears(10));
        assertEquals("Khách hàng phải từ đủ 15 tuổi.", theoO(loi(() -> service.them(treCon))).get("ngaySinh"));
        assertEquals(1, khachs.size());
    }

    @Test
    void ngaySinhKhongBatBuoc_nhungTuongLaiHoacQuaXaBiChan() {
        assertNull(service.them(req("Nguyễn An", "0912345678", "an@mail.vn")).getNgaySinh());
        KhachHangRequest tuongLai = req("Trần Bình", "0912345679", "binh@mail.vn");
        tuongLai.setNgaySinh(LocalDate.now().plusDays(1));
        assertEquals("Ngày sinh không được ở tương lai.", theoO(loi(() -> service.them(tuongLai))).get("ngaySinh"));
        KhachHangRequest quaXa = req("Lê Cường", "0912345670", "cuong@mail.vn");
        quaXa.setNgaySinh(LocalDate.of(1800, 1, 1));
        assertTrue(theoO(loi(() -> service.them(quaXa))).get("ngaySinh").contains("quá 100 tuổi"));
        KhachHangRequest tuoi100 = req("Phạm Dũng", "0912345671", "dung@mail.vn");
        tuoi100.setNgaySinh(LocalDate.now().minusYears(100));
        assertEquals(LocalDate.now().minusYears(100), service.them(tuoi100).getNgaySinh());
    }

    // ---------- sửa dùng cùng bộ luật như thêm ----------
    @Test
    void suaCungBoLuatNhuThem() {
        KhachHang a = mau();
        // bỏ trống SĐT / email, họ tên có số
        ApiException e = loi(() -> service.sua(a.getId(), req("An 2", "", "")));
        Map<String, String> o = theoO(e);
        assertEquals(3, o.size());
        // tuổi nhỏ
        KhachHangRequest nho = req("Nguyễn An", "0912345678", "an@mail.vn");
        nho.setNgaySinh(LocalDate.now().minusYears(12));
        Map<String, String> o2 = theoO(loi(() -> service.sua(a.getId(), nho)));
        assertEquals("Khách hàng phải từ đủ 15 tuổi.", o2.get("ngaySinh"));
        // không có gì bị đổi
        assertEquals("0912345678", khachs.get(0).getSdt());
        assertEquals("Nguyễn Văn An", khachs.get(0).getHoTen());
    }

    @Test
    void suaThanhCong_giuMaVaMatKhau_doiGioiTinhTrangThai() {
        KhachHang a = mau();
        String ma = a.getMaKhachHang();
        String matKhau = a.getMatKhau();
        KhachHangRequest r = req("Nguyễn Văn Ân", "0987654321", "an.moi@mail.vn");
        r.setGioiTinh(0);
        r.setTrangThai(0);
        r.setNgaySinh(LocalDate.of(2000, 5, 20));
        KhachHang sau = service.sua(a.getId(), r);
        assertEquals(ma, sau.getMaKhachHang());
        assertEquals(matKhau, sau.getMatKhau());
        assertEquals("0987654321", sau.getSdt());
        assertEquals(0, sau.getGioiTinh());
        assertEquals(0, sau.getTrangThai());
        assertEquals(LocalDate.of(2000, 5, 20), sau.getNgaySinh());
    }

    // ---------- gom lỗi nhiều ô ----------
    @Test
    void formTrongBaoLoiTatCaCacOCungLuc_loiDauTienLaCauThongBao() {
        ApiException e = loi(() -> service.them(req("", "", "")));
        Map<String, String> o = theoO(e);
        assertEquals(List.of("hoTen", "sdt", "email"), List.copyOf(o.keySet()));
        assertEquals("Vui lòng nhập họ tên khách hàng.", e.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        assertTrue(khachs.isEmpty());
    }

    @Test
    void loiDauTienLaTrungThiTraVe409() {
        mau();
        ApiException e = loi(() -> service.them(req("Trần Bình", "0912345678", "khong-hop-le")));
        assertEquals(HttpStatus.CONFLICT, e.getStatus()); // ô SĐT đứng trước và là lỗi trùng
        assertEquals("Số điện thoại đã tồn tại.", e.getMessage());
        assertEquals(2, theoO(e).size()); // SĐT trùng + email sai định dạng báo cùng lúc
    }

    @Test
    void gioiTinhTrangThaiNgoai0Va1_baoLoi() {
        KhachHangRequest r = req("Nguyễn An", "0912345678", "an@mail.vn");
        r.setGioiTinh(2);
        assertEquals("Giới tính không hợp lệ.", theoO(loi(() -> service.them(r))).get("gioiTinh"));
        r.setGioiTinh(0);
        r.setTrangThai(-1);
        assertEquals("Trạng thái không hợp lệ.", theoO(loi(() -> service.them(r))).get("trangThai"));
    }

    // ---------- mã, địa chỉ ----------
    @Test
    void maKhachHangTuDatPhaiDungDinhDangVaKhongTrung() {
        KhachHangRequest r = req("Nguyễn An", "0912345678", "an@mail.vn");
        r.setMaKhachHang("KH 01");
        assertTrue(theoO(loi(() -> service.them(r))).get("maKhachHang").contains("Mã khách hàng"));
        assertEquals("KH01", mau().getMaKhachHang()); // tự cấp khi không nhập
        KhachHangRequest trung = req("Trần Bình", "0912345679", "binh@mail.vn");
        trung.setMaKhachHang("kh01");
        ApiException e = loi(() -> service.them(trung));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        assertEquals("Mã khách hàng đã tồn tại.", theoO(e).get("maKhachHang"));
    }

    @Test
    void diaChiDaNhapThiPhaiDuTinhPhuongVaDiaChiCuThe() {
        KhachHangRequest r = req("Nguyễn An", "0912345678", "an@mail.vn");
        r.setTinhThanh("Thành phố Hà Nội");
        Map<String, String> o = theoO(loi(() -> service.them(r)));
        assertEquals("Chọn Phường/Xã.", o.get("phuongXa"));
        assertEquals("Nhập địa chỉ cụ thể (số nhà, đường...).", o.get("diaChiCuThe"));
        assertFalse(o.containsKey("tinhThanh"));
        r.setPhuongXa("Phường Hà Đông");
        r.setDiaChiCuThe("Số 15 ngõ 120 Trần Phú");
        KhachHang kh = service.them(r);
        assertEquals(1, kh.getDiaChiList().size());
        assertEquals("Số 15 ngõ 120 Trần Phú", kh.getDiaChiList().get(0).getDiaChiCuThe());
        assertEquals("0912345678", kh.getDiaChiList().get(0).getSdtNguoiNhan()); // mặc định lấy SĐT khách
        assertEquals("Nguyễn An", kh.getDiaChiList().get(0).getTenNguoiNhan());
        assertTrue(kh.getDiaChiList().get(0).getLaDiaChiMacDinh());
    }

    @Test
    void nguoiNhanDaNhapPhaiDungQuyTacNhuHoTenVaSdt_boTrongThiLayTheoKhach() {
        KhachHangRequest r = req("Nguyễn An", "0912345678", "an@mail.vn");
        r.setTinhThanh("Thành phố Hà Nội");
        r.setPhuongXa("Phường Hà Đông");
        r.setDiaChiCuThe("Số 1");
        r.setTenNguoiNhan("Mẹ 123");
        Map<String, String> o = theoO(loi(() -> service.them(r)));
        assertEquals(QuyTacDuLieu.LOI_HO_TEN_KY_TU, o.get("tenNguoiNhan"));
        assertTrue(khachs.isEmpty());

        r.setTenNguoiNhan("  Trần   Thị Mẹ ");
        r.setSdtNguoiNhan("0987654321");
        KhachHang kh = service.them(r);
        assertEquals("Trần Thị Mẹ", kh.getDiaChiList().get(0).getTenNguoiNhan());
        assertEquals("0987654321", kh.getDiaChiList().get(0).getSdtNguoiNhan());
    }

    @Test
    void nguoiNhanKhongBiKiemTraKhiKhongNhapDiaChi() {
        KhachHangRequest r = req("Nguyễn An", "0912345678", "an@mail.vn");
        r.setTenNguoiNhan("123");
        r.setSdtNguoiNhan("abc");
        assertTrue(service.them(r).getDiaChiList().isEmpty());
    }

    @Test
    void khongNhapDiaChiThiKhongTaoDiaChi() {
        assertTrue(mau().getDiaChiList().isEmpty());
    }

    // ---------- ảnh đại diện (gộp từ bản của nhóm) ----------
    @Test
    void themKhongGuiAnh_anhLaNull_guiAnhNoiBoHoacLinkThiLuuDaCat() {
        assertNull(mau().getAnhDaiDien());

        KhachHangRequest r = req("Trần Bình", "0912345679", "binh@mail.vn");
        r.setAnhDaiDien("  /api/uploads/3f2a9c.png  ");
        assertEquals("/api/uploads/3f2a9c.png", service.them(r).getAnhDaiDien());

        KhachHangRequest r2 = req("Lê Cường", "0912345670", "cuong@mail.vn");
        r2.setAnhDaiDien("https://cdn.example.com/a.jpg");
        assertEquals("https://cdn.example.com/a.jpg", service.them(r2).getAnhDaiDien());

        KhachHangRequest r3 = req("Phạm Dũng", "0912345671", "dung@mail.vn");
        r3.setAnhDaiDien("   "); // chuỗi trắng = không có ảnh
        assertNull(service.them(r3).getAnhDaiDien());
    }

    @Test
    void anhDaiDienSaiBiChanTaiOAnhDaiDien_khongLuuKhach() {
        for (String sai : List.of("javascript:alert(1)", "data:image/png;base64,AAAA", "ftp://x/a.png", "/etc/passwd",
                "/api/uploads/../x.png", "/api/uploads/a/b.png", "abc.png", "http://", "https:// co khoang trang")) {
            KhachHangRequest r = req("Nguyễn An", "0912345678", "an@mail.vn");
            r.setAnhDaiDien(sai);
            ApiException e = loi(() -> service.them(r));
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatus(), sai);
            assertTrue(theoO(e).get("anhDaiDien").contains("Ảnh đại diện"), sai);
        }
        KhachHangRequest dai = req("Nguyễn An", "0912345678", "an@mail.vn");
        dai.setAnhDaiDien("https://x.vn/" + "a".repeat(500));
        assertEquals("Đường dẫn ảnh đại diện tối đa 500 ký tự.", theoO(loi(() -> service.them(dai))).get("anhDaiDien"));
        assertTrue(khachs.isEmpty());
    }

    @Test
    void suaAnhDaiDien_nullGiuAnhCu_chuoiRongBoAnh_giaTriMoiThayAnh() {
        KhachHangRequest r = req("Nguyễn Văn An", "0912345678", "an@mail.vn");
        r.setAnhDaiDien("/api/uploads/cu.png");
        KhachHang k = service.them(r);

        KhachHangRequest khongGui = req("Nguyễn Văn An", "0912345678", "an@mail.vn"); // anhDaiDien = null
        assertEquals("/api/uploads/cu.png", service.sua(k.getId(), khongGui).getAnhDaiDien());

        KhachHangRequest thay = req("Nguyễn Văn An", "0912345678", "an@mail.vn");
        thay.setAnhDaiDien("/api/uploads/moi.webp");
        assertEquals("/api/uploads/moi.webp", service.sua(k.getId(), thay).getAnhDaiDien());

        KhachHangRequest sai = req("Nguyễn Văn An", "0912345678", "an@mail.vn");
        sai.setAnhDaiDien("javascript:alert(1)");
        assertEquals(HttpStatus.BAD_REQUEST, loi(() -> service.sua(k.getId(), sai)).getStatus());
        assertEquals("/api/uploads/moi.webp", k.getAnhDaiDien(), "sai thì giữ nguyên ảnh đang có");

        KhachHangRequest bo = req("Nguyễn Văn An", "0912345678", "an@mail.vn");
        bo.setAnhDaiDien("");
        assertNull(service.sua(k.getId(), bo).getAnhDaiDien());
    }

    @Test
    void themAnhKhongLamMatMatKhauBam() {
        KhachHangRequest r = req("Nguyễn An", "0912345678", "an@mail.vn");
        r.setAnhDaiDien("/api/uploads/a.png");
        String mk = service.them(r).getMatKhau();
        assertNotNull(mk);
        assertTrue(mk.startsWith("$2"), "mật khẩu tạm vẫn được băm BCrypt, không lưu chữ thường");
    }

    // ---------- xóa khách hàng ----------
    @Test
    void xoaKhachChuaPhatSinhGiaoDich_xoaDuoc() {
        KhachHang k = mau();
        service.xoa(k.getId());
        assertTrue(khachs.isEmpty());
    }

    @Test
    void xoaKhachDaCoHoaDon_baoLoi409DeHieu_khongLoSql() {
        KhachHang k = mau();
        khoaNgoaiChan = true;
        ApiException e = loi(() -> service.xoa(k.getId()));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        assertEquals(KhachHangService.LOI_XOA_DA_PHAT_SINH, e.getMessage());
        assertTrue(e.getMessage().contains("Ngừng hoạt động"), "gợi ý cách xử lý thay vì để lộ lỗi khóa ngoại");
        assertFalse(e.getMessage().toLowerCase().contains("fk_"));
    }

    @Test
    void xoaKhachKhongTonTai_baoKhongTim() {
        assertEquals(HttpStatus.NOT_FOUND, loi(() -> service.xoa(999)).getStatus());
    }
}
