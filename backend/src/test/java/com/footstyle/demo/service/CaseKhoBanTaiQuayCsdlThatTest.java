package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.entity.DotGiamGia;
import com.footstyle.demo.entity.DotGiamGiaChiTiet;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.NhanVien;
import com.footstyle.demo.entity.PhieuGiamGia;
import com.footstyle.demo.entity.SanPhamChiTiet;
import com.footstyle.demo.entity.VaiTro;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.DotGiamGiaChiTietRepository;
import com.footstyle.demo.repository.DotGiamGiaRepository;
import com.footstyle.demo.repository.HoaDonRepository;
import com.footstyle.demo.repository.NhanVienRepository;
import com.footstyle.demo.repository.PhieuGiamGiaRepository;
import com.footstyle.demo.repository.SanPhamChiTietRepository;
import com.footstyle.demo.security.NguoiDung;
import com.footstyle.demo.security.PhanQuyen;
import com.footstyle.demo.security.XacThuc;
import com.footstyle.demo.security.XacThucInterceptor;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 4 case khó bán tại quầy, chạy trên CSDL thật qua đúng các service của màn quầy; @Transactional nên mọi thay đổi được rollback.
 * 1. Đổi giá; 2. Phiếu hết hạn / bị sửa cho hết hạn / đổi giá trị giảm; 3. Chuyển quyền nhân viên; 4. Đợt giảm giá thay đổi.
 */
@SpringBootTest
@Transactional
class CaseKhoBanTaiQuayCsdlThatTest {

    @Autowired private HoaDonChoService cho;
    @Autowired private BanHangService banHang;
    @Autowired private GiamGiaApDungService giamGia;
    @Autowired private SanPhamChiTietRepository spctRepo;
    @Autowired private PhieuGiamGiaRepository phieuRepo;
    @Autowired private DotGiamGiaRepository dotRepo;
    @Autowired private DotGiamGiaChiTietRepository dotCtRepo;
    @Autowired private NhanVienRepository nhanVienRepo;
    @Autowired private HoaDonRepository hoaDonRepo;
    @Autowired private EntityManager em;

    private Integer idBienThe;
    private long giaGoc;

    @BeforeEach
    void chuanBi() {
        NhanVien ql = nhanVienRepo.findAll().stream()
                .filter(n -> n.getVaiTro() != null && VaiTro.TEN_QUAN_LY.equals(n.getVaiTro().getTenVaiTro())
                        && Integer.valueOf(NhanVien.HOAT_DONG).equals(n.getTrangThai()))
                .findFirst().orElseThrow();
        XacThuc.dat(XacThucInterceptor.tuNhanVien(ql));
        Map<Long, Double> dangGiam = giamGia.phanTramTheoBienThe(LocalDateTime.now());
        SanPhamChiTiet c = spctRepo.findAll().stream()
                .filter(BanHangService::dangBan)
                .filter(x -> x.getSoLuongTon() != null && x.getSoLuongTon() >= 5)
                .filter(x -> x.getGiaBan() != null && x.getGiaBan() >= 200_000)
                .filter(x -> !dangGiam.containsKey(x.getId().longValue()))
                .findFirst().orElseThrow();
        idBienThe = c.getId();
        giaGoc = c.getGiaBan();
    }

    @AfterEach
    void donDep() {
        XacThuc.xoa();
    }

    /* ----- Hàm phụ ----- */

    private Integer taoHoaDonCoHang() {
        Integer id = (Integer) cho.tao().get("id");
        cho.themSanPham(id, new HoaDonChoService.ThemSanPhamRequest(idBienThe, 1));
        return id;
    }

    private void doiGiaBan(long giaMoi) {
        SanPhamChiTiet c = spctRepo.findById(idBienThe).orElseThrow();
        c.setGiaBan(giaMoi);
        spctRepo.saveAndFlush(c);
    }

    private Map<String, Object> hoaDon(Integer id) {
        em.flush();
        em.clear();
        return cho.danhSach().stream().filter(h -> id.equals(h.get("id"))).findFirst().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> dong(Map<String, Object> h) {
        return (List<Map<String, Object>>) h.get("items");
    }

    private static BanHangService.ThanhToanChoRequest tra(String maPhieu, Long soTienGiam, long tienMat) {
        return new BanHangService.ThanhToanChoRequest(maPhieu, null, tienMat, 0L, null, null, soTienGiam, null);
    }

    private String maLoi(Integer id, BanHangService.ThanhToanChoRequest req) {
        return assertThrows(ApiException.class, () -> banHang.thanhToanHoaDonCho(id, req)).getCode();
    }

    private PhieuGiamGia taoPhieu(int phanTram) {
        PhieuGiamGia p = new PhieuGiamGia();
        p.setMaPhieu("CK" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        p.setTenPhieu("Phiếu kiểm tra case khó");
        p.setDoiTuongApDung(PhieuGiamGia.DOI_TUONG_TAT_CA);
        p.setLoaiGiam(PhieuGiamGia.LOAI_PHAN_TRAM);
        p.setGiaTriGiam((long) phanTram);
        p.setGiaTriDonHangToiThieu(0L);
        p.setSoLuong(100);
        p.setSoLuongDaDung(0);
        p.setNgayBatDau(LocalDateTime.now().minusDays(1));
        p.setNgayKetThuc(LocalDateTime.now().plusDays(1));
        p.setNgayTao(LocalDateTime.now().minusDays(1));
        p.setTrangThai(PhieuGiamGia.HOAT_DONG);
        return phieuRepo.saveAndFlush(p);
    }

    private DotGiamGia taoDot(int phanTram) {
        DotGiamGia d = new DotGiamGia();
        d.setMaDot("CK" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        d.setTenDot("Đợt kiểm tra case khó");
        d.setPhanTramGiamDot(phanTram);
        d.setNgayBatDau(LocalDateTime.now().minusDays(1));
        d.setNgayKetThuc(LocalDateTime.now().plusDays(1));
        d.setTrangThai(1);
        d.setNgayTao(LocalDateTime.now().minusDays(1));
        d = dotRepo.saveAndFlush(d);
        DotGiamGiaChiTiet ct = new DotGiamGiaChiTiet();
        ct.setDotGiamGia(d);
        ct.setIdSanPhamChiTiet(idBienThe.longValue());
        ct.setPhanTramGiamBienThe(phanTram);
        ct.setTrangThai(1);
        dotCtRepo.saveAndFlush(ct);
        return d;
    }

    /* ----- Case 1: Đổi giá ----- */

    @Test
    void case1_doiGia_hienGiaMoi_chanThanhToan_tachDong_capNhatGiaRoiMoiThanhToanDuoc() {
        Integer id = taoHoaDonCoHang();
        long giaMoi = giaGoc + 50_000;
        doiGiaBan(giaMoi);

        Map<String, Object> h = hoaDon(id);
        assertEquals(true, h.get("coGiaDoi"));
        assertEquals(true, dong(h).get(0).get("giaDoi"));
        assertEquals(giaGoc, dong(h).get(0).get("donGia"), "dòng cũ giữ giá lúc thêm");
        assertEquals(giaMoi, dong(h).get(0).get("giaHienTai"));

        assertEquals("GIA_THAY_DOI", maLoi(id, tra(null, null, giaGoc)));

        Map<String, Object> sauThem = cho.themSanPham(id, new HoaDonChoService.ThemSanPhamRequest(idBienThe, 1));
        assertEquals(2, dong(sauThem).size(), "cùng sản phẩm, giá khác thì tách dòng");
        assertTrue(String.valueOf(sauThem.get("thongBao")).contains("tách"));

        Map<String, Object> sauCapNhat = cho.capNhatGia(id);
        assertEquals(false, sauCapNhat.get("coGiaDoi"));
        assertEquals(1, dong(sauCapNhat).size(), "cùng giá thì gộp lại");
        assertEquals(2, dong(sauCapNhat).get(0).get("soLuong"));
        assertEquals(giaMoi * 2, sauCapNhat.get("tongTienHang"));

        Map<String, Object> kq = banHang.thanhToanHoaDonCho(id, tra(null, null, giaMoi * 2));
        assertEquals(giaMoi * 2, kq.get("thanhTien"));
        assertEquals(HoaDon.HOAN_THANH, hoaDonRepo.findById(id).orElseThrow().getTrangThai());
    }

    /* ----- Case 2: Sửa phiếu ----- */

    @Test
    void case2_phieuDoiGiaTriGiam_baoPhieuThayDoi_khongTuDoiTien_thanhToanLaiThiDung() {
        Integer id = taoHoaDonCoHang();
        PhieuGiamGia p = taoPhieu(10);
        long giamCu = giaGoc * 10 / 100;
        p.setGiaTriGiam(20L);
        phieuRepo.saveAndFlush(p);
        long giamMoi = giaGoc * 20 / 100;

        ApiException e = assertThrows(ApiException.class, () -> banHang.thanhToanHoaDonCho(id, tra(p.getMaPhieu(), giamCu, giaGoc - giamCu)));
        assertEquals("PHIEU_THAY_DOI", e.getCode());
        assertEquals(HoaDon.CHO_XAC_NHAN, hoaDonRepo.findById(id).orElseThrow().getTrangThai(), "chưa thanh toán");

        Map<String, Object> kq = banHang.thanhToanHoaDonCho(id, tra(p.getMaPhieu(), giamMoi, giaGoc - giamMoi));
        assertEquals(giaGoc - giamMoi, kq.get("thanhTien"));
        assertEquals(giamMoi, kq.get("soTienGiam"));
    }

    @Test
    void case2_phieuHetHanHoacBiSuaChoHetHan_baoKhongDungDuoc_vaBienMatKhoiDanhSach() {
        Integer id = taoHoaDonCoHang();
        PhieuGiamGia p = taoPhieu(10);
        long giam = giaGoc * 10 / 100;
        assertTrue(banHang.phieuChoDon(null, giaGoc).stream().anyMatch(x -> p.getMaPhieu().equals(x.get("ma"))));

        p.setNgayKetThuc(LocalDateTime.now().minusMinutes(1));
        phieuRepo.saveAndFlush(p);

        ApiException e = assertThrows(ApiException.class, () -> banHang.thanhToanHoaDonCho(id, tra(p.getMaPhieu(), giam, giaGoc - giam)));
        assertEquals("PHIEU_KHONG_HOP_LE", e.getCode());
        assertTrue(e.getMessage().contains("hết hạn"), e.getMessage());
        assertFalse(banHang.phieuChoDon(null, giaGoc).stream().anyMatch(x -> p.getMaPhieu().equals(x.get("ma"))));
        assertEquals(giaGoc, banHang.thanhToanHoaDonCho(id, tra(null, null, giaGoc)).get("thanhTien"), "bỏ phiếu thì thanh toán đủ tiền");
    }

    @Test
    void case2_phieuBiSuaNgungHoatDongHoacHetLuotHoacTangDonToiThieu_baoKhongDungDuoc() {
        Integer id = taoHoaDonCoHang();
        long giam = giaGoc * 10 / 100;

        PhieuGiamGia tat = taoPhieu(10);
        tat.setTrangThai(PhieuGiamGia.NGUNG_HOAT_DONG);
        phieuRepo.saveAndFlush(tat);
        assertEquals("PHIEU_KHONG_HOP_LE", maLoi(id, tra(tat.getMaPhieu(), giam, giaGoc - giam)));

        PhieuGiamGia het = taoPhieu(10);
        het.setSoLuong(1);
        het.setSoLuongDaDung(1);
        phieuRepo.saveAndFlush(het);
        assertEquals("PHIEU_KHONG_HOP_LE", maLoi(id, tra(het.getMaPhieu(), giam, giaGoc - giam)));

        PhieuGiamGia toiThieu = taoPhieu(10);
        toiThieu.setGiaTriDonHangToiThieu(giaGoc + 1);
        phieuRepo.saveAndFlush(toiThieu);
        assertEquals("PHIEU_KHONG_HOP_LE", maLoi(id, tra(toiThieu.getMaPhieu(), giam, giaGoc - giam)));
    }

    /* ----- Case 3: Chuyển quyền nhân viên (phần backend) ----- */

    @Test
    void case3_vaiTroDocTuCsdl_thuNganVanBanHangDuoc_nhanVienKhongVaoDuocThongKe() {
        NhanVien nv = nhanVienRepo.findAll().stream()
                .filter(n -> n.getVaiTro() != null && VaiTro.TEN_NHAN_VIEN.equals(n.getVaiTro().getTenVaiTro())).findFirst().orElseThrow();
        NguoiDung truoc = XacThucInterceptor.tuNhanVien(nv);
        assertEquals(NguoiDung.NHAN_VIEN, truoc.vaiTro());
        assertFalse(PhanQuyen.duocPhep(truoc, "GET", "/api/thong-ke/tong-quan"));

        VaiTro thuNgan = new VaiTro();
        thuNgan.setTenVaiTro(VaiTro.TEN_THU_NGAN);
        nv.setVaiTro(thuNgan);
        NguoiDung sau = XacThucInterceptor.tuNhanVien(nv);
        assertEquals(NguoiDung.THU_NGAN, sau.vaiTro());
        assertTrue(PhanQuyen.duocPhep(sau, "POST", "/api/ban-hang/hoa-don-cho"));
        assertTrue(PhanQuyen.duocPhep(sau, "GET", "/api/hoa-don"));
        assertFalse(PhanQuyen.duocPhep(sau, "GET", "/api/thong-ke/tong-quan"));
    }

    /* ----- Case 4: Đợt giảm giá thay đổi ----- */

    @Test
    void case4_dotDoiPhanTram_chanThanhToan_capNhatGiaThiDungGiaMoi() {
        DotGiamGia d = taoDot(20);
        Integer id = taoHoaDonCoHang();
        long gia20 = PhieuApDung.giaSauDot(giaGoc, 20);
        assertEquals(gia20, dong(hoaDon(id)).get(0).get("donGia"));

        DotGiamGiaChiTiet ct = dotCtRepo.findAll().stream().filter(x -> x.getDotGiamGia().getId().equals(d.getId())).findFirst().orElseThrow();
        ct.setPhanTramGiamBienThe(30);
        dotCtRepo.saveAndFlush(ct);
        long gia30 = PhieuApDung.giaSauDot(giaGoc, 30);

        Map<String, Object> h = hoaDon(id);
        assertEquals(true, h.get("coGiaDoi"));
        assertEquals(gia30, dong(h).get(0).get("giaHienTai"));
        assertEquals("GIA_THAY_DOI", maLoi(id, tra(null, null, gia20)));

        assertEquals(gia30, cho.capNhatGia(id).get("tongTienHang"));
        assertEquals(gia30, banHang.thanhToanHoaDonCho(id, tra(null, null, gia30)).get("thanhTien"));
    }

    @Test
    void case4_dotKetThucHoacBiTat_veGiaGoc_chanThanhToan() {
        DotGiamGia d = taoDot(20);
        Integer id = taoHoaDonCoHang();
        long gia20 = PhieuApDung.giaSauDot(giaGoc, 20);

        d.setNgayKetThuc(LocalDateTime.now().minusMinutes(1));
        dotRepo.saveAndFlush(d);

        Map<String, Object> h = hoaDon(id);
        assertEquals(true, h.get("coGiaDoi"));
        assertEquals(gia20, dong(h).get(0).get("donGia"));
        assertEquals(giaGoc, dong(h).get(0).get("giaHienTai"));
        assertEquals("GIA_THAY_DOI", maLoi(id, tra(null, null, gia20)));
        assertEquals(giaGoc, cho.capNhatGia(id).get("tongTienHang"));
    }

    @Test
    void case4_dotBatDauKhiHoaDonDangCho_giaMoiThapHon_chanThanhToan() {
        Integer id = taoHoaDonCoHang();
        taoDot(25);
        long gia25 = PhieuApDung.giaSauDot(giaGoc, 25);

        Map<String, Object> h = hoaDon(id);
        assertEquals(true, h.get("coGiaDoi"));
        assertEquals(giaGoc, dong(h).get(0).get("donGia"));
        assertEquals(gia25, dong(h).get(0).get("giaHienTai"));
        assertEquals("GIA_THAY_DOI", maLoi(id, tra(null, null, giaGoc)));
        assertEquals(gia25, cho.capNhatGia(id).get("tongTienHang"));
        assertEquals(gia25, banHang.thanhToanHoaDonCho(id, tra(null, null, gia25)).get("thanhTien"));
    }
}
