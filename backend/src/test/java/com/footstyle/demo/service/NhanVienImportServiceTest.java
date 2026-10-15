package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.entity.DiaChiNhanVien;
import com.footstyle.demo.entity.NhanVien;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.service.NhanVienImportService.KetQua;
import com.footstyle.demo.service.NhanVienImportService.TaiKhoanMoi;
import jakarta.mail.Message;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Nhập HÀNG LOẠT nhân viên từ Excel: file đúng thì tạo tài khoản + gửi mail; chỉ cần MỘT dòng sai là CẢ FILE bị từ chối
 * (không lưu gì, không gửi mail). Dùng file Excel thật (src/test/resources/import) và kho dữ liệu giả trong bộ nhớ.
 */
class NhanVienImportServiceTest {

    private final KhoNhanVienGia kho = new KhoNhanVienGia();
    private final MailServiceTest.GhiThu sender = new MailServiceTest.GhiThu();
    private final MailService mail = new MailService(new MailServiceTest.NoiCap(sender), "", "shop@gmail.com", "mk-ung-dung", "FootStyle",
            "https://footstyle.vn");
    private final NhanVienImportService dich = new NhanVienImportService(kho.service, kho.vaiTroRepo, mail);

    // ---------- tiện ích ----------

    private static byte[] mau(String ten) {
        return XlsxMau.nap(ten);
    }

    private ApiException tuChoi(byte[] file, boolean chiKiemTra) {
        return assertThrows(ApiException.class, () -> dich.nhap(file, chiKiemTra));
    }

    /** Lỗi của file trả về dạng "dòng:ô" -> câu báo lỗi (không phụ thuộc thứ tự). */
    @SuppressWarnings("unchecked")
    private static Map<String, String> loiTheoDong(ApiException e) {
        Map<String, String> kq = new LinkedHashMap<>();
        for (Map<String, Object> l : (List<Map<String, Object>>) e.getChiTiet().get("loi")) {
            String khoa = l.get("dong") + ":" + l.get("truong");
            assertFalse(kq.containsKey(khoa), "mỗi ô chỉ báo một lỗi: " + khoa);
            kq.put(khoa, (String) l.get("thongBao"));
        }
        return kq;
    }

    @SuppressWarnings("unchecked")
    private static String tieuDeCot(ApiException e, int dong, String truong) {
        for (Map<String, Object> l : (List<Map<String, Object>>) e.getChiTiet().get("loi")) {
            if (l.get("dong").equals(dong) && l.get("truong").equals(truong)) return (String) l.get("cot");
        }
        return null;
    }

    private Map<String, Object> doiMailXong(String maLo, int tong) throws Exception {
        Map<String, Object> kq = mail.ketQuaLo(maLo);
        for (int i = 0; i < 200 && !(Boolean.TRUE.equals(kq.get("xong")) && ((Integer) kq.get("tong")) == tong); i++) {
            Thread.sleep(25);
            kq = mail.ketQuaLo(maLo);
        }
        return kq;
    }

    private static String chuThuong(MimeMessage m) throws Exception {
        Part p = MailServiceTest.phanLoai(MailServiceTest.cacPhanLa(MailServiceTest.docLai(m)), "text/plain");
        assertNotNull(p, "thư có bản chữ");
        return (String) p.getContent();
    }

    private static String email(MimeMessage m) throws Exception {
        return ((InternetAddress) MailServiceTest.docLai(m).getRecipients(Message.RecipientType.TO)[0]).getAddress();
    }

    /** File một dòng nhân viên hợp lệ + tuỳ chỉnh một ô (cột theo thứ tự file mẫu). */
    private static byte[] motDong(int cot, Object giaTri) {
        List<Object> dong = new ArrayList<>(XlsxMau.nhanVien("Trần Văn Nam", "nam.tv@footstyle.vn", "0911222333"));
        dong.set(cot, giaTri);
        return XlsxMau.tao(XlsxMau.TIEU_DE, dong);
    }

    private static final int C_TEN = 0, C_GT = 1, C_NS = 2, C_SDT = 3, C_EMAIL = 4, C_CCCD = 5, C_VAO = 6, C_VAITRO = 7, C_DIACHI = 8, C_PHUONG = 9, C_TINH = 10;

    // ---------- file đúng ----------

    /**
     * File mẫu do GIAO DIỆN tạo ra (frontend/src/utils/mauNhapNhanVien.js, file trong test/resources được sinh trực tiếp từ code đó):
     * người dùng tải mẫu về, giữ nguyên hai dòng ví dụ và nhập thì phải được ngay. Bắt lỗi lệch tiêu đề cột giữa giao diện và backend.
     */
    @Test
    void fileMauCuaGiaoDien_nhapDuocNguyenVanTungO() {
        KetQua xem = dich.nhap(mau("nhan-vien-mau-tu-giao-dien.xlsx"), true);
        assertFalse(xem.daLuu());
        assertEquals(2, xem.soNhanVien());
        assertEquals(List.of(2, 3), xem.xemTruoc().stream().map(NhanVienImportService.XemTruoc::dong).toList());

        KetQua kq = dich.nhap(mau("nhan-vien-mau-tu-giao-dien.xlsx"), false);
        assertTrue(kq.daLuu());
        assertEquals(2, kq.soNhanVien());

        NhanVien van = kho.theoEmail("nguyen.van.mau@example.com");
        assertNotNull(van);
        assertEquals("Nguyễn Văn Mẫu", van.getHoTen());
        assertEquals(NhanVien.GIOI_TINH_NAM, van.getGioiTinh());
        assertEquals(LocalDate.of(2000, 12, 25), van.getNgaySinh(), "ngày sinh gõ dạng chữ dd/mm/yyyy");
        assertEquals("0900000001", van.getSdt(), "ô văn bản giữ nguyên số 0 đầu");
        assertEquals("001200012345", van.getCanCuocCongDan());
        assertEquals(LocalDate.now(), van.getNgayVaoLam(), "bỏ trống ngày vào làm = hôm nay");
        assertEquals("Nhân viên", van.getVaiTro().getTenVaiTro());
        assertEquals("Số 12 Nguyễn Trãi, Phường Hà Đông, Thành phố Hà Nội", van.getDiaChi());

        NhanVien du = kho.theoEmail("tran.thi.vi.du@example.com");
        assertNotNull(du);
        assertEquals("Trần Thị Ví Dụ", du.getHoTen());
        assertEquals(NhanVien.GIOI_TINH_NU, du.getGioiTinh());
        assertEquals(LocalDate.of(1998, 4, 3), du.getNgaySinh());
        assertEquals(LocalDate.of(2026, 10, 15), du.getNgayVaoLam());
        assertNull(du.getCanCuocCongDan(), "căn cước bỏ trống");
        assertEquals("Nhân viên", du.getVaiTro().getTenVaiTro(), "bỏ trống vai trò = Nhân viên (không tự thành quản lý)");

        // Nhập lần hai: hai email / SĐT đã dùng -> từ chối cả file (không ai bị thêm trùng)
        ApiException e = tuChoi(mau("nhan-vien-mau-tu-giao-dien.xlsx"), false);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, e.getStatus());
    }

    @Test
    void fileDung_taoDuNhanVienVoiMaEmailSdtCccdVaiTroChuanHoa() {
        KetQua kq = dich.nhap(mau("nhan-vien-hop-le.xlsx"), false);

        assertTrue(kq.daLuu());
        assertEquals(3, kq.soNhanVien());
        assertEquals(5, kho.nhanViens.size());
        // Mã theo họ tên; hai người cùng chữ cái đầu trong cùng file không trùng mã
        assertEquals(List.of("NVA01", "LTB01", "NVA02"), kq.taiKhoan().stream().map(TaiKhoanMoi::ma).toList());

        NhanVien an = kho.theoEmail("an.nv@footstyle.vn");
        assertNotNull(an, "email được đổi sang chữ thường");
        assertEquals("NVA01", an.getMaNhanVien());
        assertEquals("Nguyễn Văn An", an.getHoTen());
        assertEquals("0912345678", an.getSdt(), "ô SỐ 912345678 được bù lại số 0 đầu");
        assertEquals("001099012345", an.getCanCuocCongDan());
        assertEquals(LocalDate.of(1999, 5, 20), an.getNgaySinh(), "ngày sinh đọc từ số serial của Excel");
        assertEquals(LocalDate.of(2025, 1, 6), an.getNgayVaoLam());
        assertEquals(NhanVien.GIOI_TINH_NAM, an.getGioiTinh());
        assertEquals("Nhân viên", an.getVaiTro().getTenVaiTro());
        assertEquals(NhanVien.HOAT_DONG, an.getTrangThai());
        assertEquals("Số 15 ngõ 120 Trần Phú, Phường Hà Đông, Thành phố Hà Nội", an.getDiaChi());
        List<DiaChiNhanVien> dcAn = kho.diaChiCua(an);
        assertEquals(1, dcAn.size());
        assertTrue(dcAn.get(0).getMacDinh());
        assertEquals("Số 15 ngõ 120 Trần Phú", dcAn.get(0).getDiaChiCuThe());
        assertEquals("Phường Hà Đông", dcAn.get(0).getPhuongXa());
        assertEquals("Thành phố Hà Nội", dcAn.get(0).getTinhThanh());

        NhanVien binh = kho.theoEmail("binh.lt@footstyle.vn");
        assertEquals("LTB01", binh.getMaNhanVien());
        assertEquals("Quản lý", binh.getVaiTro().getTenVaiTro(), "cột Vai trò = Quản lý");
        assertEquals(NhanVien.GIOI_TINH_NU, binh.getGioiTinh());
        assertEquals(LocalDate.of(2001, 12, 25), binh.getNgaySinh(), "ngày dạng chữ dd/mm/yyyy");
        assertNull(binh.getCanCuocCongDan(), "căn cước không bắt buộc");
        LocalDate hom = LocalDate.now();
        assertFalse(binh.getNgayVaoLam().isAfter(hom), "để trống ngày vào làm thì lấy hôm nay");
        assertFalse(binh.getNgayVaoLam().isBefore(hom.minusDays(1)));

        NhanVien anh = kho.theoEmail("anh.nv@footstyle.vn");
        assertEquals("NVA02", anh.getMaNhanVien());
        assertEquals(NhanVien.GIOI_TINH_NAM, anh.getGioiTinh(), "'nam' viết thường vẫn nhận");
        assertEquals("0987654322", anh.getSdt(), "bỏ khoảng trắng trong '098 765 4322'");
        assertEquals(LocalDate.of(2000, 2, 3), anh.getNgaySinh(), "ngày dạng yyyy-mm-dd");
        assertEquals("079203001299", anh.getCanCuocCongDan(), "CCCD 11 chữ số (Excel làm mất số 0) được bù lại thành 12");
        assertEquals("Nhân viên", anh.getVaiTro().getTenVaiTro(), "bỏ trống vai trò thì là Nhân viên");
    }

    @Test
    void matKhauTamNgauNhienHopLeVaDuocBamKhongLuuBanRo() {
        KetQua kq = dich.nhap(mau("nhan-vien-hop-le.xlsx"), false);
        Set<String> daThay = new HashSet<>();
        for (TaiKhoanMoi tk : kq.taiKhoan()) {
            NhanVien nv = kho.theoEmail(tk.email());
            assertEquals(MatKhauTam.DO_DAI, tk.matKhauTam().length());
            assertTrue(tk.matKhauTam().matches(".*[A-Za-z].*") && tk.matKhauTam().matches(".*\\d.*"), "đủ luật mật khẩu: có chữ và số");
            assertNotEquals(tk.matKhauTam(), nv.getMatKhau(), "không lưu mật khẩu gốc");
            assertTrue(kho.bam.matches(tk.matKhauTam(), nv.getMatKhau()), "mật khẩu đã băm khớp với mật khẩu tạm gửi cho nhân viên");
            assertTrue(daThay.add(tk.matKhauTam()), "mỗi nhân viên một mật khẩu riêng");
        }
        assertEquals(3, daThay.size());
    }

    @Test
    void nhapXongTuGuiMailTaiKhoanChoTungNhanVien() throws Exception {
        KetQua kq = dich.nhap(mau("nhan-vien-hop-le.xlsx"), false);
        assertNotNull(kq.maLo());
        assertEquals(3, kq.soMail());
        assertTrue(kq.mailDaCauHinh());

        Map<String, Object> ketQuaMail = doiMailXong(kq.maLo(), 3);
        assertEquals(3, ketQuaMail.get("tong"));
        assertEquals(3, ketQuaMail.get("daGui"));
        assertEquals(0, ketQuaMail.get("loi"));
        assertEquals(true, ketQuaMail.get("xong"));
        assertEquals(3, dich.ketQuaMail(kq.maLo()).get("daGui"), "ketQuaMail của dịch vụ nhập trả đúng kết quả của lô");

        assertEquals(3, sender.daGui.size());
        for (TaiKhoanMoi tk : kq.taiKhoan()) {
            MimeMessage thu = sender.daGui.stream().filter(m -> {
                try {
                    return email(m).equals(tk.email());
                } catch (Exception e) {
                    return false;
                }
            }).findFirst().orElseThrow(() -> new AssertionError("không có thư gửi " + tk.email()));
            assertEquals("[FootStyle] Tài khoản nhân viên của bạn đã được tạo", thu.getSubject());
            String chu = chuThuong(thu);
            assertTrue(chu.contains(tk.ma()), "thư có mã nhân viên");
            assertTrue(chu.contains(tk.matKhauTam()), "thư có mật khẩu tạm");
            assertTrue(chu.contains(tk.email()), "tài khoản đăng nhập là email");
            assertTrue(chu.contains(tk.vaiTro()));
            assertTrue(chu.contains(tk.hoTen()));
        }
    }

    @Test
    void guiMailLoi_nhanVienVanDuocTaoVaBaoRoLyDo() throws Exception {
        sender.loi = true; // Gmail trả 535 sai mật khẩu ứng dụng
        KetQua kq = dich.nhap(mau("nhan-vien-hop-le.xlsx"), false);
        assertEquals(3, kq.soNhanVien());
        assertEquals(5, kho.nhanViens.size(), "gửi mail lỗi không làm mất nhân viên đã tạo");
        Map<String, Object> ketQuaMail = doiMailXong(kq.maLo(), 3);
        assertEquals(3, ketQuaMail.get("loi"));
        assertEquals(0, ketQuaMail.get("daGui"));
        assertTrue(ketQuaMail.get("lyDo").toString().contains("535"));
        // Mật khẩu tạm vẫn có trong kết quả trả về để quản lý gửi lại cho nhân viên
        assertEquals(3, kq.taiKhoan().stream().filter(t -> t.matKhauTam() != null && !t.matKhauTam().isEmpty()).count());
    }

    @Test
    void chuaCauHinhMail_vanNhapDuocVaBaoLaChuaGuiThat() throws Exception {
        MailService chuaCauHinh = new MailService(new MailServiceTest.NoiCap(null), "", "", "", "FootStyle", "");
        NhanVienImportService s = new NhanVienImportService(kho.service, kho.vaiTroRepo, chuaCauHinh);
        KetQua kq = s.nhap(mau("nhan-vien-hop-le.xlsx"), false);
        assertTrue(kq.daLuu());
        assertFalse(kq.mailDaCauHinh(), "giao diện cần biết để báo 'chưa gửi mail thật'");
        Map<String, Object> m = chuaCauHinh.ketQuaLo(kq.maLo());
        for (int i = 0; i < 200 && !Boolean.TRUE.equals(m.get("xong")); i++) {
            Thread.sleep(25);
            m = chuaCauHinh.ketQuaLo(kq.maLo());
        }
        assertEquals(3, m.get("giaLap"));
        assertEquals(0, m.get("daGui"));
        assertEquals(0, sender.soLanGoi.get(), "không có bộ gửi thật nên không gửi gì");
    }

    @Test
    void chiKiemTra_fileDung_khongLuuKhongGuiMailVaXemTruocDungMaSeCap() {
        KetQua xem = dich.nhap(mau("nhan-vien-hop-le.xlsx"), true);
        assertFalse(xem.daLuu());
        assertEquals(3, xem.soNhanVien());
        assertTrue(xem.taiKhoan().isEmpty(), "chưa tạo tài khoản nên chưa có mật khẩu tạm");
        assertNull(xem.maLo());
        assertEquals(List.of(2, 3, 4), xem.xemTruoc().stream().map(NhanVienImportService.XemTruoc::dong).toList());
        assertEquals(List.of("NVA01", "LTB01", "NVA02"), xem.xemTruoc().stream().map(NhanVienImportService.XemTruoc::ma).toList());
        assertEquals(List.of("an.nv@footstyle.vn", "binh.lt@footstyle.vn", "anh.nv@footstyle.vn"),
                xem.xemTruoc().stream().map(NhanVienImportService.XemTruoc::email).toList());
        assertEquals(List.of("Nhân viên", "Quản lý", "Nhân viên"), xem.xemTruoc().stream().map(NhanVienImportService.XemTruoc::vaiTro).toList());
        assertEquals(2, kho.nhanViens.size(), "chỉ kiểm tra: chưa thêm ai");
        assertEquals(0, kho.soLanLuu);
        assertEquals(0, kho.diaChis.size());
        assertEquals(0, sender.soLanGoi.get());

        // Nhập thật sau đó cho đúng những mã đã xem trước
        KetQua that = dich.nhap(mau("nhan-vien-hop-le.xlsx"), false);
        assertEquals(xem.xemTruoc().stream().map(NhanVienImportService.XemTruoc::ma).toList(),
                that.taiKhoan().stream().map(TaiKhoanMoi::ma).toList());
    }

    @Test
    void dongTrongGiuaFile_boQuaVaGiuDungSoDongThatCuaExcel() {
        KetQua xem = dich.nhap(mau("nhan-vien-hop-le-shared.xlsx"), true);
        assertEquals(List.of(3, 5), xem.xemTruoc().stream().map(NhanVienImportService.XemTruoc::dong).toList(), "dòng 3 và 5 trong Excel");
        assertEquals(2, xem.soNhanVien());
    }

    // ---------- một dòng sai là từ chối cả file ----------

    @Test
    void fileCoDongSai_tuChoiCaFile422KemDanhSachLoiTungDongTungCot() {
        ApiException e = tuChoi(mau("nhan-vien-co-loi.xlsx"), false);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, e.getStatus());
        assertEquals(NhanVienImportService.MA_LOI_FILE, e.getCode());
        Map<String, String> loi = loiTheoDong(e);
        assertEquals(10, loi.size(), loi.toString());
        assertEquals(QuyTacDuLieu.LOI_SDT, loi.get("3:soDienThoai"));
        assertEquals("Email trùng với dòng 2 trong file (mỗi email chỉ được dùng một lần).", loi.get("4:email"));
        assertEquals("Nhân viên phải từ đủ 18 tuổi.", loi.get("5:ngaySinh"));
        assertEquals("Email đã được sử dụng.", loi.get("6:email"));
        assertEquals("Ngày sinh không đúng định dạng (viết dd/mm/yyyy, ví dụ 25/12/2000).", loi.get("7:ngaySinh"));
        assertEquals("Vai trò chỉ gồm Quản lý hoặc Nhân viên.", loi.get("8:idVaiTro"));
        assertEquals("Nhập địa chỉ cụ thể (số nhà, đường...).", loi.get("9:diaChiCuThe"));
        assertEquals(QuyTacDuLieu.LOI_HO_TEN_KY_TU, loi.get("10:hoTen"));
        assertEquals("Số điện thoại trùng với dòng 4 trong file (mỗi số điện thoại chỉ được dùng một lần).", loi.get("11:soDienThoai"));
        assertEquals("Số căn cước công dân đã được sử dụng.", loi.get("11:canCuoc"));
        assertFalse(loi.keySet().stream().anyMatch(k -> k.startsWith("2:")), "dòng 2 hợp lệ nên không báo lỗi");

        // Mỗi lỗi ghi rõ tiêu đề cột trong file để người dùng tìm đúng ô
        assertEquals("Số điện thoại", tieuDeCot(e, 3, "soDienThoai"));
        assertEquals("Email", tieuDeCot(e, 4, "email"));
        assertEquals("Ngày sinh", tieuDeCot(e, 7, "ngaySinh"));
        assertEquals("Vai trò", tieuDeCot(e, 8, "idVaiTro"));
        assertEquals("Địa chỉ cụ thể", tieuDeCot(e, 9, "diaChiCuThe"));
        assertEquals("Họ và tên", tieuDeCot(e, 10, "hoTen"));
        assertEquals("Số căn cước công dân", tieuDeCot(e, 11, "canCuoc"));

        assertEquals(10, e.getChiTiet().get("tongDong"));
        assertEquals(9, e.getChiTiet().get("soDongLoi"));
        assertTrue(e.getMessage().contains("10 lỗi ở 9/10 dòng"), e.getMessage());
        assertTrue(e.getMessage().contains("Cả file bị từ chối"), e.getMessage());
    }

    @Test
    void fileCoDongSai_khongLuuBatCuAiKhongGuiMail_duDongHopLeNamTruoc() throws Exception {
        tuChoi(mau("nhan-vien-co-loi.xlsx"), false);
        // Dòng 2 hợp lệ (Nguyễn Văn An) cũng KHÔNG được lưu
        assertEquals(2, kho.nhanViens.size());
        assertNull(kho.theoEmail("an.nv@footstyle.vn"));
        assertEquals(0, kho.soLanLuu);
        assertEquals(0, kho.diaChis.size());
        Thread.sleep(50);
        assertEquals(0, sender.soLanGoi.get(), "không gửi mail nào");
    }

    @Test
    void fileCoDongSai_chiKiemTraCungTuChoiNhuNhapThat() {
        ApiException e = tuChoi(mau("nhan-vien-co-loi.xlsx"), true);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, e.getStatus());
        assertEquals(10, loiTheoDong(e).size());
    }

    @Test
    void nhapLaiCungMotFile_biChanVoiLiDoTrungVaKhongNhanDoi() {
        dich.nhap(mau("nhan-vien-hop-le.xlsx"), false);
        assertEquals(5, kho.nhanViens.size());

        ApiException e = tuChoi(mau("nhan-vien-hop-le.xlsx"), false);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, e.getStatus());
        Map<String, String> loi = loiTheoDong(e);
        assertEquals("Email đã được sử dụng.", loi.get("2:email"));
        assertEquals("Số điện thoại đã được sử dụng.", loi.get("2:soDienThoai"));
        assertEquals("Số căn cước công dân đã được sử dụng.", loi.get("2:canCuoc"));
        assertEquals("Email đã được sử dụng.", loi.get("3:email"));
        assertEquals("Email đã được sử dụng.", loi.get("4:email"));
        assertEquals(3, e.getChiTiet().get("soDongLoi"), "cả 3 dòng đều trùng");
        assertEquals(5, kho.nhanViens.size(), "không nhân đôi nhân viên");
    }

    @Test
    void thieuCotBatBuoc_400ChiRoCotThieu() {
        ApiException e = tuChoi(mau("nhan-vien-thieu-cot.xlsx"), false);
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        assertTrue(e.getMessage().contains("thiếu cột bắt buộc: Email."), e.getMessage());
        assertEquals(2, kho.nhanViens.size());
    }

    @Test
    void chiCoDongTieuDe_400() {
        ApiException e = tuChoi(mau("nhan-vien-chi-co-tieu-de.xlsx"), false);
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        assertTrue(e.getMessage().contains("chưa có dòng nhân viên"), e.getMessage());
    }

    @Test
    void sheetTrongHoanToan_400() {
        ApiException e = tuChoi(XlsxMau.tao(List.of()), false);
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        assertTrue(e.getMessage().contains("đang trống"), e.getMessage());
    }

    @Test
    void khongPhaiExcel_400() {
        ApiException e = tuChoi("Họ tên,Email\nAn,an@x.vn".getBytes(java.nio.charset.StandardCharsets.UTF_8), false);
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        assertTrue(e.getMessage().contains(".xlsx"));
    }

    // ---------- giới hạn số dòng ----------

    private static List<List<Object>> nhieuDong(int n) {
        List<List<Object>> ds = new ArrayList<>();
        ds.add(XlsxMau.TIEU_DE);
        for (int i = 1; i <= n; i++) {
            // Họ tên khác nhau nhưng cùng chữ cái đầu (N-V-A) nên mã là NVA01...NVA100
            String ten = "A" + (char) ('a' + i / 26) + (char) ('a' + i % 26);
            ds.add(XlsxMau.nhanVien("Nguyễn Văn " + ten, "nv" + i + "@footstyle.vn", String.format(java.util.Locale.ROOT, "09%08d", i)));
        }
        return ds;
    }

    @Test
    void toiDa100NhanVienMoiFile() {
        KetQua vua = dich.nhap(XlsxMau.tao(nhieuDong(100)), true);
        assertEquals(100, vua.soNhanVien());
        assertEquals(100, vua.xemTruoc().stream().map(NhanVienImportService.XemTruoc::ma).distinct().count(), "100 mã khác nhau");
        assertEquals("NVA100", vua.xemTruoc().get(99).ma(), "quá 99 thì viết thẳng số, không đệm 0");

        ApiException e = tuChoi(XlsxMau.tao(nhieuDong(101)), true);
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        assertTrue(e.getMessage().contains("101") && e.getMessage().contains("tối đa 100"), e.getMessage());
    }

    // ---------- nhận diện cột ----------

    @Test
    void tieuDeKhongDauKhacThuTuKhongCoDauSao_vanNhanDuoc() {
        // email | ho ten | Gioi Tinh | SDT | ngay sinh | Dia chi | phuong | tinh/thanh pho
        byte[] f = XlsxMau.tao(
                Arrays.asList("email", "ho ten", "Gioi Tinh", "SDT", "ngay sinh", "Dia chi", "phuong", "tinh/thanh pho"),
                Arrays.asList("lan@footstyle.vn", "Phạm Thị Lan", "Nữ", "0361234567", "01/02/1998", "12 Lê Lợi", "Phường Bến Nghé", "Thành phố Hồ Chí Minh"));
        KetQua kq = dich.nhap(f, false);
        assertEquals(1, kq.soNhanVien());
        NhanVien lan = kho.theoEmail("lan@footstyle.vn");
        assertEquals("PTL01", lan.getMaNhanVien());
        assertEquals("0361234567", lan.getSdt());
        assertEquals(LocalDate.of(1998, 2, 1), lan.getNgaySinh());
        assertEquals("12 Lê Lợi, Phường Bến Nghé, Thành phố Hồ Chí Minh", lan.getDiaChi());
        assertEquals("Nhân viên", lan.getVaiTro().getTenVaiTro(), "không có cột Vai trò thì mặc định là Nhân viên");
    }

    @Test
    void cotLaTrongFileDuocBoQua() {
        List<Object> tieuDe = new ArrayList<>(XlsxMau.TIEU_DE);
        tieuDe.add("Ghi chú của tôi");
        tieuDe.add("Mật khẩu");
        List<Object> dong = new ArrayList<>(XlsxMau.nhanVien("Trần Văn Nam", "nam.tv@footstyle.vn", "0911222333"));
        dong.add("cột phụ");
        dong.add("MatKhau123"); // mật khẩu không nhập từ file: hệ thống tự sinh mật khẩu tạm
        KetQua kq = dich.nhap(XlsxMau.tao(tieuDe, dong), false);
        assertEquals(1, kq.soNhanVien());
        assertNotEquals("MatKhau123", kq.taiKhoan().get(0).matKhauTam());
        assertFalse(kho.bam.matches("MatKhau123", kho.theoEmail("nam.tv@footstyle.vn").getMatKhau()));
    }

    // ---------- từng loại lỗi của một ô ----------

    @Test
    void dongBoTrongNhieuONhieuLoi_baoHetCungLucDungOTungCot() {
        List<Object> chiCoTen = Arrays.asList("Trần Văn Nam", null, null, null, null, null, null, null, null, null, null);
        ApiException e = tuChoi(XlsxMau.tao(XlsxMau.TIEU_DE, chiCoTen), false);
        Map<String, String> loi = loiTheoDong(e);
        assertEquals("Chọn giới tính.", loi.get("2:gioiTinh"));
        assertEquals("Nhập ngày sinh.", loi.get("2:ngaySinh"));
        assertEquals("Nhập số điện thoại.", loi.get("2:soDienThoai"));
        assertEquals("Nhập email.", loi.get("2:email"));
        assertEquals("Nhập Phường/Xã.", loi.get("2:phuongXa"));
        assertEquals("Nhập Tỉnh/Thành phố.", loi.get("2:tinhThanh"));
        assertEquals("Nhập địa chỉ cụ thể (số nhà, đường...).", loi.get("2:diaChiCuThe"));
        assertEquals(7, loi.size(), loi.toString());
        assertEquals("Phường/Xã", tieuDeCot(e, 2, "phuongXa"));
        assertEquals("Tỉnh/Thành phố", tieuDeCot(e, 2, "tinhThanh"));
        assertEquals("Giới tính", tieuDeCot(e, 2, "gioiTinh"));
        assertTrue(e.getMessage().contains("7 lỗi ở 1/1 dòng"));
    }

    @Test
    void soDongBaoLoiLaSoDongThatCuaExcel_duCoDongTrongPhiaTruoc() {
        List<Object> sai = XlsxMau.nhanVien("Trần Văn Nam", "nam.tv@footstyle.vn", "0211222333");
        ApiException e = tuChoi(XlsxMau.tao(XlsxMau.TIEU_DE, List.of(), List.of(), sai), false);
        assertEquals(List.of("4:soDienThoai"), List.copyOf(loiTheoDong(e).keySet()));
    }

    @Test
    void ngayVaoLamTuongLaiBiChan() {
        ApiException e = tuChoi(motDong(C_VAO, LocalDate.now().plusDays(3)), false);
        assertEquals("Ngày vào làm không được ở tương lai.", loiTheoDong(e).get("2:ngayVaoLam"));
        assertEquals("Ngày vào làm", tieuDeCot(e, 2, "ngayVaoLam"));
    }

    @Test
    void ngayVaoLamTruocKhiDu18Tuoi_biChan() {
        // Sinh 20/05/1999 nhưng "vào làm" 01/01/2015 (mới 15 tuổi)
        ApiException e = tuChoi(motDong(C_VAO, "01/01/2015"), false);
        assertTrue(loiTheoDong(e).get("2:ngayVaoLam").contains("từ đủ 18 tuổi"), loiTheoDong(e).toString());
    }

    @Test
    void tuoiDungMoc18_homNayDuoc_ngayMaiThiChua() {
        LocalDate hom = LocalDate.now();
        KetQua ok = dich.nhap(motDong(C_NS, hom.minusYears(18)), true);
        assertEquals(1, ok.soNhanVien(), "đủ 18 tuổi đúng hôm nay là hợp lệ");
        ApiException e = tuChoi(motDong(C_NS, hom.minusYears(18).plusDays(1)), true);
        assertEquals("Nhân viên phải từ đủ 18 tuổi.", loiTheoDong(e).get("2:ngaySinh"));
    }

    @Test
    void ngaySinhQuaTuoi_vaTuongLai() {
        ApiException e = tuChoi(motDong(C_NS, "01/01/1900"), true);
        assertTrue(loiTheoDong(e).get("2:ngaySinh").contains("quá 100 tuổi"));
        ApiException e2 = tuChoi(motDong(C_NS, LocalDate.now().plusDays(1)), true);
        assertEquals("Ngày sinh không được ở tương lai.", loiTheoDong(e2).get("2:ngaySinh"));
    }

    @Test
    void cacKieuNgayChuHopLe_vaKhongHopLe() {
        for (String ngay : new String[] {"5/3/2000", "05/03/2000", "5-3-2000", "5.3.2000", "2000-03-05", "2000/3/5", "29/02/2000", "2000-03-05T00:00:00"}) {
            assertEquals(1, dich.nhap(motDong(C_NS, ngay), true).soNhanVien(), "phải nhận: " + ngay);
        }
        for (String sai : new String[] {"29/02/2001", "31/04/2000", "1/13/2000", "00/01/2000", "hôm qua", "2000", "05/03/00", "12/25/2000"}) {
            ApiException e = tuChoi(motDong(C_NS, sai), true);
            assertTrue(loiTheoDong(e).get("2:ngaySinh").startsWith("Ngày sinh không đúng định dạng"), sai + " -> " + loiTheoDong(e));
        }
        // Ô số không phải ngày (0, âm, quá lớn)
        for (Number so : new Number[] {0, -5, 99999999}) {
            ApiException e = tuChoi(motDong(C_NS, so), true);
            assertTrue(loiTheoDong(e).get("2:ngaySinh").startsWith("Ngày sinh không đúng định dạng"), so + " -> " + loiTheoDong(e));
        }
    }

    @Test
    void emailSai_vaTrungKhongPhanBietHoaThuongTrongFile() {
        ApiException e = tuChoi(motDong(C_EMAIL, "khong-phai-email"), true);
        assertTrue(loiTheoDong(e).get("2:email").contains("Email chưa đúng định dạng"));

        byte[] hai = XlsxMau.tao(XlsxMau.TIEU_DE, XlsxMau.nhanVien("Lan Anh", "Lan@footstyle.vn", "0911000001"),
                XlsxMau.nhanVien("Mai Lan", "  lan@FOOTSTYLE.vn ", "0911000002"));
        ApiException e2 = tuChoi(hai, true);
        assertEquals("Email trùng với dòng 2 trong file (mỗi email chỉ được dùng một lần).", loiTheoDong(e2).get("3:email"));
        assertEquals(1, loiTheoDong(e2).size());
    }

    @Test
    void sdtDaCoTrongHeThong_vaDauSoSai_vaSoThieuChuSo() {
        ApiException trung = tuChoi(motDong(C_SDT, "0912000001"), true); // của NV01
        assertEquals("Số điện thoại đã được sử dụng.", loiTheoDong(trung).get("2:soDienThoai"));
        for (Object sdt : new Object[] {"0212345678", "091234567", "09123456789", "+84912345678", "09123abc78", 912345}) {
            ApiException e = tuChoi(motDong(C_SDT, sdt), true);
            assertEquals(QuyTacDuLieu.LOI_SDT, loiTheoDong(e).get("2:soDienThoai"), String.valueOf(sdt));
        }
        // Chuẩn: 10 số đầu 03/05/07/08/09, có thể gõ dạng số (mất số 0 đầu) hoặc có khoảng trắng / dấu chấm
        for (Object sdt : new Object[] {"0351234567", "0501234567", "0781234567", "0891234567", "0911 222 333", "0911.222.333", "0911-222-333", 911222333, 351234567}) {
            assertEquals(1, dich.nhap(motDong(C_SDT, sdt), true).soNhanVien(), "phải nhận: " + sdt);
        }
    }

    @Test
    void canCuocSaiDoDaiHoacChuaChu_vaCanCuocSoDu12ChuSo() {
        for (Object cccd : new Object[] {"12345", "00109901234a", "0010990123456", 123L}) {
            ApiException e = tuChoi(motDong(C_CCCD, cccd), true);
            assertEquals("Số căn cước công dân gồm đúng 12 chữ số.", loiTheoDong(e).get("2:canCuoc"), String.valueOf(cccd));
        }
        assertEquals(1, dich.nhap(motDong(C_CCCD, 123456789012L), true).soNhanVien());
        assertEquals(1, dich.nhap(motDong(C_CCCD, "001 099 012 345"), true).soNhanVien());
    }

    @Test
    void hoTenKhoangTrangThuaDuocChuanHoa_vaTenSaiBiChan() {
        KetQua kq = dich.nhap(motDong(C_TEN, "  Trần    Văn   Nam  "), false);
        assertEquals("Trần Văn Nam", kho.theoEmail("nam.tv@footstyle.vn").getHoTen());
        assertEquals("TVN01", kq.taiKhoan().get(0).ma());
        for (String ten : new String[] {"Nam 2", "N", "123", "Nam@", "_Nam"}) {
            ApiException e = tuChoi(motDong(C_TEN, ten), true);
            assertFalse(loiTheoDong(e).get("2:hoTen").isEmpty(), ten);
        }
    }

    @Test
    void gioiTinhVaVaiTro_chiNhanGiaTriHopLe() {
        for (String gt : new String[] {"Nam", "nam", " NAM ", "Nữ", "nu", "NỮ"}) {
            assertEquals(1, dich.nhap(motDong(C_GT, gt), true).soNhanVien(), gt);
        }
        ApiException e = tuChoi(motDong(C_GT, "Khác"), true);
        assertEquals("Giới tính không hợp lệ.", loiTheoDong(e).get("2:gioiTinh"));

        for (String vt : new String[] {"quản lý", "QUẢN LÝ", "Quan ly", "nhân viên", "Nhan Vien"}) {
            assertEquals(1, dich.nhap(motDong(C_VAITRO, vt), true).soNhanVien(), vt);
        }
        for (String vt : new String[] {"Kế toán", "Admin", "Đã ngưng"}) {
            ApiException e2 = tuChoi(motDong(C_VAITRO, vt), true);
            assertEquals("Vai trò chỉ gồm Quản lý hoặc Nhân viên.", loiTheoDong(e2).get("2:idVaiTro"), vt);
        }
    }

    @Test
    void nhieuLoiTrongCungMotFile_baoDuTatCaDongKhongDungOLoiDauTien() {
        // 3 dòng, mỗi dòng một lỗi khác nhau: phải thấy cả 3 chứ không dừng ở dòng 2
        byte[] f = XlsxMau.tao(XlsxMau.TIEU_DE,
                XlsxMau.nhanVien("Nam 1", "a@footstyle.vn", "0911000001"),
                XlsxMau.nhanVien("Nam Hai", "b-khong-hop-le", "0911000002"),
                XlsxMau.nhanVien("Nam Ba", "c@footstyle.vn", "0211000003"));
        ApiException e = tuChoi(f, true);
        assertEquals(List.of("2:hoTen", "3:email", "4:soDienThoai"), List.copyOf(loiTheoDong(e).keySet()));
        assertEquals(3, e.getChiTiet().get("soDongLoi"));
    }

    @Test
    void ketQuaMailCuaLoKhongTonTai_laXongVoiTongBangKhong() {
        Map<String, Object> m = dich.ketQuaMail("NVIMP-khong-co");
        assertEquals(0, m.get("tong"));
        assertEquals(true, m.get("xong"));
    }

    @Test
    void nhapHaiLanLienTiepKhacFile_maLoKhongDeLenNhau() throws Exception {
        KetQua a = dich.nhap(motDong(C_TEN, "Trần Văn Nam"), false);
        Thread.sleep(3); // mã lô theo mili giây
        KetQua b = dich.nhap(XlsxMau.tao(XlsxMau.TIEU_DE, XlsxMau.nhanVien("Lê Văn Bình", "binh@footstyle.vn", "0911000009")), false);
        assertNotEquals(a.maLo(), b.maLo());
        assertEquals("TVN01", a.taiKhoan().get(0).ma());
        assertEquals("LVB01", b.taiKhoan().get(0).ma());
        assertEquals(1, doiMailXong(a.maLo(), 1).get("daGui"));
        assertEquals(1, doiMailXong(b.maLo(), 1).get("daGui"));
    }

    @Test
    void khongTuTaoQuyenKhac_chiHaiVaiTroDuocDung() {
        // Vai trò "Kế toán" có thật trong CSDL và đang dùng, nhưng hệ thống chỉ có 2 quyền: không được nhập
        ApiException e = tuChoi(motDong(C_VAITRO, "Kế toán"), false);
        assertEquals(1, loiTheoDong(e).size());
        assertEquals(2, kho.nhanViens.size());
        assertEquals(2, kho.service.getVaiTro().size());
    }

    @Test
    void duLieuNgoaiCotNhanDienKhongLamHongViecDocDong() {
        // Dòng có ô dư ở cột xa (cột T) vẫn đọc bình thường
        List<Object> dong = new ArrayList<>(XlsxMau.nhanVien("Trần Văn Nam", "nam.tv@footstyle.vn", "0911222333"));
        while (dong.size() < 19) dong.add(null);
        dong.add("ô dư ở cột T");
        List<Object> tieuDe = new ArrayList<>(XlsxMau.TIEU_DE);
        assertEquals(1, dich.nhap(XlsxMau.tao(tieuDe, dong), true).soNhanVien());
    }
}
