package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.dto.DiaChiNhanVienDto;
import com.footstyle.demo.dto.NhanVienRequest;
import com.footstyle.demo.dto.NhanVienResponse;
import com.footstyle.demo.entity.DiaChiNhanVien;
import com.footstyle.demo.entity.NhanVien;
import com.footstyle.demo.entity.VaiTro;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.DiaChiNhanVienRepository;
import com.footstyle.demo.repository.NhanVienRepository;
import com.footstyle.demo.repository.VaiTroRepository;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Kiểm tra NhanVienService thật với kho dữ liệu giả trong bộ nhớ (không cần CSDL, Spring hay Mockito):
 * mã theo tên (LQT01), CCCD, nhiều địa chỉ, sửa nhân viên.
 */
class NhanVienServiceTest {

    // ---------- kho dữ liệu giả ----------
    private final List<NhanVien> nhanViens = new ArrayList<>();
    private final List<DiaChiNhanVien> diaChis = new ArrayList<>();
    private final List<VaiTro> vaiTros = new ArrayList<>();
    private int idNv = 100;
    private int idDc = 500;
    private final NhanVienService service;

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Function<Object[], Object> khac, java.util.function.BiFunction<String, Object[], Object> xuLy) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (p, m, args) -> {
            Object kq = xuLy.apply(m.getName(), args == null ? new Object[0] : args);
            if (kq == NONE) throw new UnsupportedOperationException(m.getName());
            return kq;
        });
    }

    private static final Object NONE = new Object();

    NhanVienServiceTest() {
        VaiTro quanLy = vaiTro(1, VaiTro.TEN_QUAN_LY, 1);
        VaiTro nhanVien = vaiTro(2, "Nhân viên", 1);
        vaiTro(3, "Đã ngưng", 0);
        // 2 nhân viên mẫu: 1 quản lý (NV01) + 1 nhân viên (NV02)
        NhanVien a = nv("NV01", "Nguyễn Hoàng Long", "0912000001", "long@footstyle.vn", quanLy);
        a.setCanCuocCongDan("079203001234");
        nv("NV02", "Trần Thị Mai", "0912000002", "mai@footstyle.vn", nhanVien);

        NhanVienRepository nvRepo = proxy(NhanVienRepository.class, null, (ten, a1) -> switch (ten) {
            case "findAllByOrderByIdDesc" -> nhanViens.stream().sorted(Comparator.comparing(NhanVien::getId).reversed()).toList();
            case "findAll" -> new ArrayList<>(nhanViens);
            case "findById" -> nhanViens.stream().filter(x -> x.getId().equals(a1[0])).findFirst();
            case "existsByEmailIgnoreCaseAndIdNot" -> nhanViens.stream().anyMatch(x -> x.getEmail() != null && x.getEmail().equalsIgnoreCase((String) a1[0]) && !x.getId().equals(a1[1]));
            case "existsBySdtAndIdNot" -> nhanViens.stream().anyMatch(x -> a1[0].equals(x.getSdt()) && !x.getId().equals(a1[1]));
            case "existsByCanCuocCongDanAndIdNot" -> nhanViens.stream().anyMatch(x -> a1[0].equals(x.getCanCuocCongDan()) && !x.getId().equals(a1[1]));
            case "countByVaiTroIdAndTrangThai" -> nhanViens.stream().filter(x -> x.getVaiTro().getId().equals(a1[0]) && a1[1].equals(x.getTrangThai())).count();
            case "findAllMaNhanVien" -> nhanViens.stream().map(NhanVien::getMaNhanVien).toList();
            case "save" -> {
                NhanVien x = (NhanVien) a1[0];
                if (x.getId() == null) {
                    x.setId(++idNv);
                    nhanViens.add(x);
                }
                yield x;
            }
            default -> NONE;
        });
        DiaChiNhanVienRepository dcRepo = proxy(DiaChiNhanVienRepository.class, null, (ten, a1) -> switch (ten) {
            case "findByNhanVienIdOrderByMacDinhDescIdAsc" -> diaChis.stream().filter(d -> d.getNhanVien().getId().equals(a1[0]))
                    .sorted(Comparator.comparing((DiaChiNhanVien d) -> !Boolean.TRUE.equals(d.getMacDinh())).thenComparing(DiaChiNhanVien::getId)).toList();
            case "saveAll" -> {
                List<DiaChiNhanVien> kq = new ArrayList<>();
                for (Object o : (Iterable<?>) a1[0]) {
                    DiaChiNhanVien d = (DiaChiNhanVien) o;
                    if (d.getId() == null) {
                        d.setId(++idDc);
                        diaChis.add(d);
                    }
                    kq.add(d);
                }
                yield kq;
            }
            default -> NONE;
        });
        VaiTroRepository vtRepo = proxy(VaiTroRepository.class, null, (ten, a1) -> switch (ten) {
            case "findById" -> vaiTros.stream().filter(x -> x.getId().equals(a1[0])).findFirst();
            case "findByTrangThaiOrderByIdAsc" -> vaiTros.stream().filter(x -> x.getTrangThai().equals(a1[0])).toList();
            default -> NONE;
        });
        service = new NhanVienService(nvRepo, dcRepo, vtRepo, new BCryptPasswordEncoder());
    }

    private VaiTro vaiTro(int id, String ten, int trangThai) {
        VaiTro v = new VaiTro();
        v.setId(id);
        v.setTenVaiTro(ten);
        v.setTrangThai(trangThai);
        vaiTros.add(v);
        return v;
    }

    private NhanVien nv(String ma, String hoTen, String sdt, String email, VaiTro vaiTro) {
        NhanVien n = new NhanVien();
        n.setId(nhanViens.size() + 1);
        n.setMaNhanVien(ma);
        n.setHoTen(hoTen);
        n.setSdt(sdt);
        n.setEmail(email);
        n.setVaiTro(vaiTro);
        n.setTrangThai(NhanVien.HOAT_DONG);
        n.setDiaChi("Hà Nội");
        n.setNgayVaoLam(LocalDate.of(2022, 1, 10));
        nhanViens.add(n);
        return n;
    }

    // ---------- tiện ích dựng request ----------
    private static DiaChiNhanVienDto dc(Integer id, String tinh, String phuong, String chiTiet, boolean chinh) {
        return new DiaChiNhanVienDto(id, tinh, phuong, chiTiet, chinh);
    }

    private static NhanVienRequest them(String hoTen, String sdt, String email, String canCuoc, List<DiaChiNhanVienDto> diaChis) {
        return new NhanVienRequest(hoTen, "Nam", LocalDate.of(1999, 5, 20), sdt, email, null, LocalDate.of(2024, 1, 1), 2, "Footstyle@123", canCuoc, diaChis);
    }

    private static List<DiaChiNhanVienDto> mot() {
        return List.of(dc(null, "Thành phố Hà Nội", "Phường Hà Đông", "Số 15 ngõ 120 Trần Phú", true));
    }

    private static ApiException loi(Runnable r) {
        return assertThrows(ApiException.class, r::run);
    }

    // ---------- mã nhân viên ----------
    @Test
    void maTheoTenLQT0001RoiLQT0002() {
        NhanVienResponse a = service.them(them("Lê Quốc Tuấn", "0987000001", "tuan@footstyle.vn", null, mot()));
        assertEquals("LQT01", a.ma());
        NhanVienResponse b = service.them(them("  lê   quang  tú ", "0987000002", "tu@footstyle.vn", null, mot()));
        assertEquals("LQT02", b.ma());
        assertEquals("lê quang tú".replaceAll("\\s+", " "), b.hoTen());
        // mã mẫu giữ nguyên
        assertTrue(service.getAll().stream().anyMatch(x -> "NV01".equals(x.ma())));
        assertTrue(service.getAll().stream().anyMatch(x -> "NV02".equals(x.ma())));
    }

    @Test
    void nguoiCoChuCaiDauNVDanhSoTiepKhongTrungMaMau() {
        NhanVienResponse r = service.them(them("Ngô Vinh", "0987000003", "vinh@footstyle.vn", null, mot()));
        assertEquals("NV03", r.ma());
    }

    // ---------- CCCD ----------
    @Test
    void canCuocLuuDuocVaKhongTrungKhongSaiDoDai() {
        NhanVienResponse r = service.them(them("Lê Quốc Tuấn", "0987000001", "tuan@footstyle.vn", " 001099012345 ", mot()));
        assertEquals("001099012345", r.canCuoc());
        assertEquals(HttpStatus.CONFLICT, loi(() -> service.them(them("Phạm An", "0987000002", "an@footstyle.vn", "001099012345", mot()))).getStatus());
        assertEquals(HttpStatus.CONFLICT, loi(() -> service.them(them("Phạm An", "0987000002", "an@footstyle.vn", "079203001234", mot()))).getStatus()); // trùng NV01
        assertEquals(HttpStatus.BAD_REQUEST, loi(() -> service.them(them("Phạm An", "0987000002", "an@footstyle.vn", "12345", mot()))).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, loi(() -> service.them(them("Phạm An", "0987000002", "an@footstyle.vn", "00109901234a", mot()))).getStatus());
        NhanVienResponse khong = service.them(them("Phạm An", "0987000002", "an@footstyle.vn", "  ", mot()));
        assertNull(khong.canCuoc());
    }

    // ---------- địa chỉ ----------
    @Test
    void nhieuDiaChiDung1DiaChiChinhVaDiaChiGhepMotDong() {
        List<DiaChiNhanVienDto> ds = List.of(
                dc(null, "Thành phố Hà Nội", "Phường Hà Đông", "Số 15 ngõ 120 Trần Phú", false),
                dc(null, "Tỉnh Hưng Yên", "Phường Thái Bình", "Số 3 Lý Bôn", true),
                dc(null, null, null, "Địa chỉ cũ chỉ có chuỗi", true)); // hai cái cùng đánh dấu chính: chỉ lấy cái đầu tiên
        NhanVienResponse r = service.them(them("Lê Quốc Tuấn", "0987000001", "tuan@footstyle.vn", null, ds));
        assertEquals(3, r.diaChis().size());
        assertEquals(1, r.diaChis().stream().filter(DiaChiNhanVienDto::macDinh).count());
        assertEquals("Số 3 Lý Bôn", r.diaChis().get(0).diaChiCuThe()); // địa chỉ chính đứng đầu
        assertEquals("Số 3 Lý Bôn, Phường Thái Bình, Tỉnh Hưng Yên", r.diaChi());
    }

    @Test
    void khongChonDiaChiChinhThiLayDiaChiDauTien() {
        List<DiaChiNhanVienDto> ds = List.of(dc(null, "Thành phố Hà Nội", "Phường A", "Số 1", false), dc(null, "Thành phố Hà Nội", "Phường B", "Số 2", false));
        NhanVienResponse r = service.them(them("Lê Quốc Tuấn", "0987000001", "tuan@footstyle.vn", null, ds));
        assertEquals("Số 1, Phường A, Thành phố Hà Nội", r.diaChi());
        assertEquals(1, r.diaChis().stream().filter(DiaChiNhanVienDto::macDinh).count());
    }

    @Test
    void thieuDiaChiHoacDiaChiCuTheThiBaoLoiVaKhongLuuGi() {
        int truoc = nhanViens.size();
        assertEquals("Nhập ít nhất một địa chỉ.", loi(() -> service.them(them("Lê Quốc Tuấn", "0987000001", "tuan@footstyle.vn", null, List.of()))).getMessage());
        assertEquals("Nhập ít nhất một địa chỉ.", loi(() -> service.them(them("Lê Quốc Tuấn", "0987000001", "tuan@footstyle.vn", null, null))).getMessage());
        assertTrue(loi(() -> service.them(them("Lê Quốc Tuấn", "0987000001", "tuan@footstyle.vn", null, List.of(dc(null, "Thành phố Hà Nội", "Phường A", "   ", true))))).getMessage().contains("địa chỉ thứ 1"));
        assertEquals(truoc, nhanViens.size());
        assertEquals(0, diaChis.size());
    }

    @Test
    void banCuChiGuiMotChuoiDiaChiVanDuocXemLaDiaChiChinh() {
        NhanVienRequest req = new NhanVienRequest("Lê Quốc Tuấn", "Nam", LocalDate.of(1999, 5, 20), "0987000001", "tuan@footstyle.vn", "Số 8 Lê Văn Lương, Hà Nội", LocalDate.of(2024, 1, 1), 2, "Footstyle@123", null, null);
        NhanVienResponse r = service.them(req);
        assertEquals("Số 8 Lê Văn Lương, Hà Nội", r.diaChi());
        assertEquals(1, r.diaChis().size());
        assertTrue(r.diaChis().get(0).macDinh());
    }

    // ---------- các kiểm tra cũ vẫn giữ ----------
    @Test
    void matKhauDuocBamVaThongTinTrungBiChan() {
        service.them(them("Lê Quốc Tuấn", "0987000001", "Tuan@Footstyle.vn", null, mot()));
        NhanVien daLuu = nhanViens.get(nhanViens.size() - 1);
        assertEquals("tuan@footstyle.vn", daLuu.getEmail());
        assertFalse("Footstyle@123".equals(daLuu.getMatKhau()));
        assertEquals(HttpStatus.CONFLICT, loi(() -> service.them(them("Phạm An", "0987000009", "TUAN@footstyle.vn", null, mot()))).getStatus());
        assertEquals(HttpStatus.CONFLICT, loi(() -> service.them(them("Phạm An", "0987000001", "an@footstyle.vn", null, mot()))).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, loi(() -> service.them(new NhanVienRequest("Phạm An", "Nam", LocalDate.now().minusYears(10), "0987000007", "an@footstyle.vn", null, null, 2, "Footstyle@123", null, mot()))).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, loi(() -> service.them(new NhanVienRequest("Phạm An", "Nam", null, "0987000007", "an@footstyle.vn", null, null, 3, "Footstyle@123", null, mot()))).getStatus()); // vai trò ngưng dùng
    }

    // ---------- sửa ----------
    private NhanVienResponse taoMau() {
        return service.them(them("Lê Quốc Tuấn", "0987000001", "tuan@footstyle.vn", "001099012345", List.of(
                dc(null, "Tỉnh Hưng Yên", "Phường Thái Bình", "Số 3 Lý Bôn", true),
                dc(null, "Thành phố Hà Nội", "Phường Hà Đông", "Số 15 Trần Phú", false))));
    }

    @Test
    void suaGiuNguyenMaEmailNgayVaoLamDoiDiaChiChinhVaThemDiaChi() {
        NhanVienResponse goc = taoMau();
        Integer id1 = goc.diaChis().get(0).id();
        Integer id2 = goc.diaChis().get(1).id();
        List<DiaChiNhanVienDto> ds = List.of(
                dc(id1, "Tỉnh Hưng Yên", "Phường Thái Bình", "Số 3 Lý Bôn", false),
                dc(id2, "Thành phố Hà Nội", "Phường Hà Đông", "Số 15 Trần Phú (sửa)", true),
                dc(null, "Thành phố Hà Nội", "Phường Hồng Hà", "Số 9 Bạch Đằng", false));
        NhanVienRequest req = new NhanVienRequest("Lê Quốc Tuấn B", "Nam", LocalDate.of(1999, 5, 20), "0987000001", "khac@x.vn", null, LocalDate.of(2000, 1, 1), 2, "matkhaumoi123", "001099012345", ds);
        NhanVienResponse sau = service.sua(goc.id(), req);
        assertEquals("LQT01", sau.ma()); // mã không đổi dù đổi tên
        assertEquals("tuan@footstyle.vn", sau.email()); // email không đổi
        assertEquals(LocalDate.of(2024, 1, 1), sau.ngayVaoLam()); // ngày vào làm không đổi
        assertEquals("001099012345", sau.canCuoc()); // giữ CCCD của chính mình được
        assertEquals(3, sau.diaChis().size());
        assertEquals(id2, sau.diaChis().get(0).id()); // địa chỉ chính mới đứng đầu
        assertEquals("Số 15 Trần Phú (sửa), Phường Hà Đông, Thành phố Hà Nội", sau.diaChi());
        assertEquals(1, sau.diaChis().stream().filter(DiaChiNhanVienDto::macDinh).count());
        assertEquals(3, diaChis.stream().filter(d -> d.getNhanVien().getId().equals(goc.id())).count());
    }

    @Test
    void suaKhongXoaDiaChiKhongGuiLenVaChanDiaChiCuaNguoiKhac() {
        NhanVienResponse goc = taoMau();
        Integer id1 = goc.diaChis().get(0).id();
        NhanVienRequest chiGuiMot = new NhanVienRequest("Lê Quốc Tuấn", "Nam", LocalDate.of(1999, 5, 20), "0987000001", null, null, null, 2, null, "001099012345", List.of(dc(id1, "Tỉnh Hưng Yên", "Phường Thái Bình", "Số 3 Lý Bôn", true)));
        NhanVienResponse sau = service.sua(goc.id(), chiGuiMot);
        assertEquals(2, sau.diaChis().size()); // địa chỉ thứ hai vẫn còn
        // địa chỉ của nhân viên khác
        NhanVienResponse khac = service.them(them("Phạm An", "0987000002", "an@footstyle.vn", null, mot()));
        Integer idCuaKhac = khac.diaChis().get(0).id();
        NhanVienRequest sai = new NhanVienRequest("Lê Quốc Tuấn", "Nam", LocalDate.of(1999, 5, 20), "0987000001", null, null, null, 2, null, "001099012345", List.of(dc(idCuaKhac, null, null, "X", true)));
        assertEquals("Địa chỉ không thuộc nhân viên này.", loi(() -> service.sua(goc.id(), sai)).getMessage());
    }

    @Test
    void suaChanCCCDVaSdtCuaNguoiKhac() {
        NhanVienResponse goc = taoMau();
        NhanVienRequest trungCccd = new NhanVienRequest("Lê Quốc Tuấn", "Nam", LocalDate.of(1999, 5, 20), "0987000001", null, null, null, 2, null, "079203001234", mot());
        assertEquals(HttpStatus.CONFLICT, loi(() -> service.sua(goc.id(), trungCccd)).getStatus());
        NhanVienRequest trungSdt = new NhanVienRequest("Lê Quốc Tuấn", "Nam", LocalDate.of(1999, 5, 20), "0912000002", null, null, null, 2, null, "001099012345", mot());
        assertEquals(HttpStatus.CONFLICT, loi(() -> service.sua(goc.id(), trungSdt)).getStatus());
    }

    @Test
    void khongDoiVaiTroCuaQuanLyCuoiCung() {
        NhanVienRequest doiVaiTro = new NhanVienRequest("Nguyễn Hoàng Long", "Nam", LocalDate.of(1992, 3, 14), "0912000001", null, null, null, 2, null, "079203001234", mot());
        assertEquals(HttpStatus.CONFLICT, loi(() -> service.sua(1, doiVaiTro)).getStatus());
    }

    @Test
    void khongKhoaQuanLyCuoiCungNhungKhoaVaMoKhoaNhanVienThuongDuoc() {
        assertEquals(HttpStatus.CONFLICT, loi(() -> service.doiTrangThai(1)).getStatus());
        assertFalse(service.doiTrangThai(2).hoatDong());
        assertTrue(service.doiTrangThai(2).hoatDong());
    }

    @Test
    void nhanVienKhongTimThayBaoLoi404() {
        assertEquals(HttpStatus.NOT_FOUND, loi(() -> service.getById(9999)).getStatus());
    }

    // ---------- case khó ----------
    @Test
    void ngaySinhQua100TuoiLaGoNhamNam_100TuoiVanDuoc() {
        LocalDate homNay = LocalDate.now();
        NhanVienRequest gia = new NhanVienRequest("Lê Quốc Tuấn", "Nam", homNay.minusYears(101), "0987000001", "tuan@footstyle.vn", null,
                homNay, 2, "Footstyle@123", null, mot());
        assertTrue(loi(() -> service.them(gia)).getMessage().contains("quá 100 tuổi"));
        NhanVienRequest dung100 = new NhanVienRequest("Lê Quốc Tuấn", "Nam", homNay.minusYears(101).plusDays(1), "0987000001", "tuan@footstyle.vn", null,
                homNay, 2, "Footstyle@123", null, mot());
        assertEquals("LQT01", service.them(dung100).ma());
    }

    @Test
    void cungMotDiaChiGuiHaiLan_baoLoiKhongDeDe() {
        NhanVienResponse goc = taoMau();
        Integer id1 = goc.diaChis().get(0).id();
        NhanVienRequest trung = new NhanVienRequest("Lê Quốc Tuấn", "Nam", LocalDate.of(1999, 5, 20), "0987000001", null, null, null, 2, null,
                "001099012345", List.of(dc(id1, null, null, "Số 1", true), dc(id1, null, null, "Số 2", false)));
        assertTrue(loi(() -> service.sua(goc.id(), trung)).getMessage().contains("trùng"));
    }

    @Test
    void sinhNgay29Thang2_du18TuoiDungNgay() {
        // Sinh 29/02/2008: đủ 18 tuổi vào 28/02/2026 (Java cộng năm rơi về ngày cuối tháng 2)
        NhanVienRequest r = new NhanVienRequest("Đỗ Hải", "Nam", LocalDate.of(2008, 2, 29), "0987000009", "hai@footstyle.vn", null,
                LocalDate.of(2026, 2, 27), 2, "Footstyle@123", null, mot());
        assertTrue(loi(() -> service.them(r)).getMessage().contains("Ngày vào làm"));
        NhanVienRequest ok = new NhanVienRequest("Đỗ Hải", "Nam", LocalDate.of(2008, 2, 29), "0987000009", "hai@footstyle.vn", null,
                LocalDate.of(2026, 2, 28), 2, "Footstyle@123", null, mot());
        assertEquals("DH01", service.them(ok).ma());
    }

    @Test
    void emailKhacHoaThuongVanTinhLaTrung() {
        NhanVienRequest r = them("Phạm An", "0987000002", "  LONG@FootStyle.VN ", null, mot());
        assertEquals(HttpStatus.CONFLICT, loi(() -> service.them(r)).getStatus());
    }
}
