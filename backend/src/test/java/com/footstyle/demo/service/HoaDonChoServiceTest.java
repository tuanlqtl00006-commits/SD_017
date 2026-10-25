package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.KhoGia;
import com.footstyle.demo.entity.DanhMuc;
import com.footstyle.demo.entity.GiaoCa;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.HoaDonChiTiet;
import com.footstyle.demo.entity.LichSuHoaDon;
import com.footstyle.demo.entity.LichSuThanhToan;
import com.footstyle.demo.entity.MauSac;
import com.footstyle.demo.entity.SanPham;
import com.footstyle.demo.entity.SanPhamChiTiet;
import com.footstyle.demo.entity.ThuongHieu;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.HinhAnhSanPhamRepository;
import com.footstyle.demo.repository.HoaDonChiTietRepository;
import com.footstyle.demo.repository.HoaDonRepository;
import com.footstyle.demo.repository.KhachHangRepository;
import com.footstyle.demo.repository.LichSuHoaDonRepository;
import com.footstyle.demo.repository.LichSuThanhToanRepository;
import com.footstyle.demo.repository.PhieuGiamGiaKhachHangRepository;
import com.footstyle.demo.repository.PhieuGiamGiaRepository;
import com.footstyle.demo.repository.SanPhamChiTietRepository;
import com.footstyle.demo.security.XacThuc;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** Hóa đơn chờ tại quầy lưu ở CSDL: thêm là trừ kho, bỏ / hủy là trả kho, đổi giá thì tách dòng, thanh toán (kèm giao hàng). */
class HoaDonChoServiceTest {

    private static final LocalDateTime LUC = CaLamMau.HOM_NAY.atTime(9, 0);

    private final CaLamMau m = new CaLamMau();
    private final List<SanPhamChiTiet> bienThes = new ArrayList<>();
    private final List<HoaDonChiTiet> chiTiets = new ArrayList<>();
    private final List<LichSuHoaDon> lichSus = new ArrayList<>();
    private final Map<Long, Double> phanTramDot = new HashMap<>();
    private final SanPhamChiTiet vot; // 4.290.000, tồn 5
    private final SanPhamChiTiet cau; // 250.000, tồn 2
    private final BanHangService banHang;
    private final HoaDonChoService service;
    private int idChiTiet = 1;

    HoaDonChoServiceTest() {
        DanhMuc votCauLong = danhMuc(1, "Vợt cầu lông");
        DanhMuc phuKien = danhMuc(2, "Phụ kiện");
        vot = bienThe(1, sanPham(1, "SP01", "Vợt Yonex Astrox 99 Pro", votCauLong), "SP01-DEN-4U-G5", "Đen", 4_290_000L, 5);
        cau = bienThe(2, sanPham(2, "SP07", "Ống cầu Thành Công", phuKien), "SP07-TRANG", "Trắng", 250_000L, 2);
        bienThe(3, sanPham(3, "SP09", "Balo Yonex", phuKien), "SP09-DEN", "Đen", 990_000L, 0); // hết hàng

        GiamGiaApDungService giam = new GiamGiaApDungService(null) {
            @Override
            public Map<Long, Double> phanTramTheoBienThe(LocalDateTime thoiDiem) {
                return phanTramDot;
            }
        };
        GiaoCaService giaoCa = new GiaoCaService(m.giaoCaRepo(), m.nhanVienRepo(), m.lichRepo(), hoaDonRepo(), thanhToanRepo(), m.phuongThucRepo());
        HoaDonChiTietRepository ctRepo = chiTietRepo();
        LichSuHoaDonRepository lsRepo = lichSuRepo();
        banHang = new BanHangService(spctRepo(), KhoGia.tao(HinhAnhSanPhamRepository.class, (t, a) -> "findByTrangThaiOrderByLaAnhChinhDescIdAsc".equals(t) ? List.of() : KhoGia.KHONG_CO),
                KhoGia.tao(KhachHangRepository.class, (t, a) -> KhoGia.KHONG_CO), m.nhanVienRepo(),
                KhoGia.tao(PhieuGiamGiaRepository.class, (t, a) -> "findAll".equals(t) ? List.of() : KhoGia.KHONG_CO),
                KhoGia.tao(PhieuGiamGiaKhachHangRepository.class, (t, a) -> KhoGia.KHONG_CO),
                hoaDonRepo(), ctRepo, lsRepo, thanhToanRepo(), m.phuongThucRepo(), giam, giaoCa);
        service = new HoaDonChoService(hoaDonRepo(), ctRepo, spctRepo(), KhoGia.tao(KhachHangRepository.class, (t, a) -> KhoGia.KHONG_CO),
                m.nhanVienRepo(), lsRepo, giaoCa, giam, banHang);
        Clock dongHo = Clock.fixed(LUC.atZone(CaLamMau.VN).toInstant(), CaLamMau.VN);
        banHang.datDongHo(dongHo);
        service.datDongHo(dongHo);
        moCa();
    }

    @AfterEach
    void don() {
        XacThuc.xoa();
    }

    /* ---------- dữ liệu mẫu ---------- */

    private static DanhMuc danhMuc(int id, String ten) {
        DanhMuc d = new DanhMuc();
        d.setId(id);
        d.setTen(ten);
        return d;
    }

    private static SanPham sanPham(int id, String ma, String ten, DanhMuc dm) {
        SanPham sp = new SanPham();
        sp.setId(id);
        sp.setMaSanPham(ma);
        sp.setTenSanPham(ten);
        ThuongHieu th = new ThuongHieu();
        th.setId(id);
        th.setTen("Yonex");
        sp.setThuongHieu(th);
        sp.setDanhMuc(dm);
        sp.setTrangThai(1);
        return sp;
    }

    private SanPhamChiTiet bienThe(int id, SanPham sp, String ma, String mau, long gia, int ton) {
        SanPhamChiTiet c = new SanPhamChiTiet();
        c.setId(id);
        c.setSanPham(sp);
        c.setMaSpct(ma);
        MauSac ms = new MauSac();
        ms.setId(id);
        ms.setTen(mau);
        c.setMauSac(ms);
        c.setGiaBan(gia);
        c.setSoLuongTon(ton);
        c.setTrangThai(1);
        bienThes.add(c);
        return c;
    }

    private void moCa() {
        GiaoCa g = new GiaoCa();
        g.setId(7);
        g.setMaGiaoCa("GC07");
        g.setNhanVien(m.bao);
        g.setThoiGianNhanCa(LUC.minusHours(2));
        g.setTienDauCa(BigDecimal.ZERO);
        g.setTrangThai(GiaoCa.DANG_TRONG_CA);
        m.giaoCas.add(g);
        CaLamMau.dangNhap(m.bao);
    }

    /* ---------- kho giả ---------- */

    private SanPhamChiTietRepository spctRepo() {
        return KhoGia.tao(SanPhamChiTietRepository.class, (t, a) -> switch (t) {
            case "findAll" -> new ArrayList<>(bienThes);
            case "findAllByOrderByIdDesc" -> bienThes.stream().sorted((x, y) -> y.getId() - x.getId()).toList();
            case "findById" -> bienThes.stream().filter(c -> c.getId().equals(a[0])).findFirst();
            case "truTonKho" -> {
                SanPhamChiTiet c = bienThes.stream().filter(x -> x.getId().equals(a[0])).findFirst().orElseThrow();
                int sl = (Integer) a[1];
                if (c.getSoLuongTon() < sl) yield 0;
                c.setSoLuongTon(c.getSoLuongTon() - sl);
                yield 1;
            }
            case "congTonKho" -> {
                SanPhamChiTiet c = bienThes.stream().filter(x -> x.getId().equals(a[0])).findFirst().orElseThrow();
                c.setSoLuongTon(c.getSoLuongTon() + (Integer) a[1]);
                yield 1;
            }
            default -> KhoGia.KHONG_CO;
        });
    }

    private HoaDonRepository hoaDonRepo() {
        return KhoGia.tao(HoaDonRepository.class, (t, a) -> switch (t) {
            case "findAllMaHoaDon" -> m.hoaDons.stream().map(HoaDon::getMaHoaDon).toList();
            case "findById" -> m.hoaDons.stream().filter(h -> h.getId().equals(a[0])).findFirst();
            case "saveAndFlush", "save" -> {
                HoaDon h = (HoaDon) a[0];
                if (h.getId() == null) {
                    h.setId(m.hoaDons.size() + 100);
                    m.hoaDons.add(h);
                }
                yield h;
            }
            case "delete" -> m.hoaDons.remove(a[0]);
            case "findByNhanVien_IdAndLoaiDonAndTrangThaiOrderByNgayTaoAscIdAsc" -> m.hoaDons.stream()
                    .filter(h -> h.getNhanVien() != null && h.getNhanVien().getId().equals(a[0]))
                    .filter(h -> Objects.equals(h.getLoaiDon(), a[1]) && Objects.equals(h.getTrangThai(), a[2])).toList();
            case "findByIdGiaoCaOrderByNgayTaoAsc" -> m.hoaDons.stream().filter(h -> Objects.equals(h.getIdGiaoCa(), a[0])).toList();
            case "countByKhachHang_IdAndPhieuGiamGia_IdAndTrangThaiNot" -> 0L;
            default -> KhoGia.KHONG_CO;
        });
    }

    private HoaDonChiTietRepository chiTietRepo() {
        return KhoGia.tao(HoaDonChiTietRepository.class, (t, a) -> switch (t) {
            case "save" -> {
                HoaDonChiTiet ct = (HoaDonChiTiet) a[0];
                if (ct.getId() == null) {
                    ct.setId(idChiTiet++);
                    chiTiets.add(ct);
                }
                yield ct;
            }
            case "delete" -> chiTiets.remove(a[0]);
            case "findByHoaDon_IdOrderByIdAsc" -> chiTiets.stream().filter(ct -> ct.getHoaDon().getId().equals(a[0])).toList();
            default -> KhoGia.KHONG_CO;
        });
    }

    private LichSuHoaDonRepository lichSuRepo() {
        return KhoGia.tao(LichSuHoaDonRepository.class, (t, a) -> switch (t) {
            case "save" -> {
                lichSus.add((LichSuHoaDon) a[0]);
                yield a[0];
            }
            case "findByHoaDon_IdOrderByThoiGianAscIdAsc" -> lichSus.stream().filter(l -> l.getHoaDon().getId().equals(a[0])).toList();
            case "deleteAll" -> {
                for (Object o : (Iterable<?>) a[0]) lichSus.remove(o);
                yield null;
            }
            default -> KhoGia.KHONG_CO;
        });
    }

    private LichSuThanhToanRepository thanhToanRepo() {
        return KhoGia.tao(LichSuThanhToanRepository.class, (t, a) -> switch (t) {
            case "save" -> {
                m.thanhToans.add((LichSuThanhToan) a[0]);
                yield a[0];
            }
            case "findByHoaDon_IdGiaoCa" -> m.thanhToans.stream().filter(x -> Objects.equals(x.getHoaDon().getIdGiaoCa(), a[0])).toList();
            default -> KhoGia.KHONG_CO;
        });
    }

    /* ---------- tiện ích ---------- */

    private static ApiException loi(Runnable r) {
        return assertThrows(ApiException.class, r::run);
    }

    private Integer taoHoaDon() {
        return (Integer) service.tao().get("id");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> items(Map<String, Object> hd) {
        return (List<Map<String, Object>>) hd.get("items");
    }

    private static BanHangService.ThanhToanChoRequest tra(long tienMat, BanHangService.GiaoHangRequest giao, Boolean xacNhanGia) {
        return new BanHangService.ThanhToanChoRequest(null, null, tienMat, 0L, null, xacNhanGia, null, giao);
    }

    /* ---------- kiểm thử ---------- */

    @Test
    void themSanPham_truKhoNgay_taoChiTiet_congDonCungGia() {
        Integer id = taoHoaDon();
        service.themSanPham(id, new HoaDonChoService.ThemSanPhamRequest(vot.getId(), 2));
        Map<String, Object> hd = service.themSanPham(id, new HoaDonChoService.ThemSanPhamRequest(vot.getId(), 1));
        assertEquals(2, vot.getSoLuongTon(), "5 - 3: kho trừ ngay khi thêm vào hóa đơn chờ");
        assertEquals(1, items(hd).size(), "cùng giá thì cộng dồn vào một dòng");
        assertEquals(3, items(hd).get(0).get("soLuong"));
        assertEquals(12_870_000L, hd.get("tongTienHang"));
        HoaDon trongCsdl = m.hoaDons.stream().filter(h -> h.getId().equals(id)).findFirst().orElseThrow();
        assertEquals(HoaDon.CHO_XAC_NHAN, trongCsdl.getTrangThai());
        assertEquals(7, trongCsdl.getIdGiaoCa());
    }

    @Test
    void themQuaTonKho_baoHetHang_khongTruKho() {
        Integer id = taoHoaDon();
        ApiException e = loi(() -> service.themSanPham(id, new HoaDonChoService.ThemSanPhamRequest(cau.getId(), 3)));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        assertEquals("HET_HANG", e.getCode());
        assertTrue(e.getMessage().contains("chỉ còn 2"));
        assertEquals(2, cau.getSoLuongTon());
        assertTrue(chiTiets.isEmpty());
    }

    @Test
    void doiGiaRoiThemCungSanPham_tachThanhHaiDong_capNhatGiaThiGopLai() {
        Integer id = taoHoaDon();
        service.themSanPham(id, new HoaDonChoService.ThemSanPhamRequest(vot.getId(), 1));
        phanTramDot.put(1L, 10.0); // đợt giảm giá 10% vừa bắt đầu
        Map<String, Object> hd = service.themSanPham(id, new HoaDonChoService.ThemSanPhamRequest(vot.getId(), 1));
        assertEquals(2, items(hd).size(), "giá khác thì 2 bản ghi hóa đơn chi tiết");
        assertEquals(4_290_000L, items(hd).get(0).get("donGia"));
        assertEquals(true, items(hd).get(0).get("giaDoi"));
        assertEquals(3_861_000L, items(hd).get(1).get("donGia"));
        assertTrue(String.valueOf(hd.get("thongBao")).contains("dòng mới"));

        ApiException e = loi(() -> banHang.thanhToanHoaDonCho(id, tra(10_000_000L, null, null)));
        assertEquals("GIA_THAY_DOI", e.getCode(), "chưa xác nhận giá mới thì chưa cho thanh toán");

        hd = service.capNhatGia(id);
        assertEquals(1, items(hd).size(), "cùng biến thể cùng giá thì gộp lại");
        assertEquals(2, items(hd).get(0).get("soLuong"));
        assertEquals(7_722_000L, hd.get("tongTienHang"));
    }

    @Test
    void doiSoLuong_xoaDong_huy_traLaiKho() {
        Integer id = taoHoaDon();
        Map<String, Object> hd = service.themSanPham(id, new HoaDonChoService.ThemSanPhamRequest(vot.getId(), 1));
        Integer idCt = (Integer) items(hd).get(0).get("id");
        service.doiSoLuong(id, idCt, new HoaDonChoService.SoLuongRequest(4));
        assertEquals(1, vot.getSoLuongTon());
        assertEquals("HET_HANG", loi(() -> service.doiSoLuong(id, idCt, new HoaDonChoService.SoLuongRequest(7))).getCode());
        service.doiSoLuong(id, idCt, new HoaDonChoService.SoLuongRequest(2));
        assertEquals(3, vot.getSoLuongTon(), "giảm số lượng thì trả bớt về kho");
        service.themSanPham(id, new HoaDonChoService.ThemSanPhamRequest(cau.getId(), 2));
        service.xoaDong(id, idCt);
        assertEquals(5, vot.getSoLuongTon());
        service.huy(id);
        assertEquals(2, cau.getSoLuongTon(), "hủy hóa đơn chờ trả hết hàng về kho");
        HoaDon h = m.hoaDons.stream().filter(x -> x.getId().equals(id)).findFirst().orElseThrow();
        assertEquals(HoaDon.DA_HUY, h.getTrangThai());
        assertEquals("HOA_DON_CHO_DA_DONG", loi(() -> service.themSanPham(id, new HoaDonChoService.ThemSanPhamRequest(vot.getId(), 1))).getCode());
    }

    @Test
    void huyHoaDonRong_xoaHan() {
        Integer id = taoHoaDon();
        service.huy(id);
        assertTrue(m.hoaDons.stream().noneMatch(h -> h.getId().equals(id)));
        assertTrue(service.danhSach().isEmpty());
    }

    @Test
    void thanhToanTaiQuay_hoanThanhNgay_khongTruKhoLanHai() {
        Integer id = taoHoaDon();
        service.themSanPham(id, new HoaDonChoService.ThemSanPhamRequest(cau.getId(), 2));
        Map<String, Object> kq = banHang.thanhToanHoaDonCho(id, tra(600_000L, null, null));
        assertEquals(500_000L, kq.get("thanhTien"));
        assertEquals(100_000L, kq.get("tienThua"));
        assertEquals(0, cau.getSoLuongTon(), "kho đã trừ lúc thêm, thanh toán không trừ thêm");
        HoaDon h = m.hoaDons.stream().filter(x -> x.getId().equals(id)).findFirst().orElseThrow();
        assertEquals(HoaDon.HOAN_THANH, h.getTrangThai());
        assertEquals(HoaDon.TAI_QUAY, h.getLoaiDon());
        assertEquals(1, m.thanhToans.size());
        assertTrue(service.danhSach().isEmpty());
    }

    @Test
    void thanhToanCoGiaoHang_tinhPhiShip_sangChoGiaoHang_khongCanXacNhan() {
        Integer id = taoHoaDon();
        service.themSanPham(id, new HoaDonChoService.ThemSanPhamRequest(cau.getId(), 1));
        ApiException e = loi(() -> banHang.thanhToanHoaDonCho(id, tra(300_000L, new BanHangService.GiaoHangRequest(" ", "123", "ngắn"), null)));
        assertEquals(List.of("hoTen", "sdt", "diaChi"), List.copyOf(e.getLoiTheoTruong().keySet()));
        Map<String, Object> kq = banHang.thanhToanHoaDonCho(id, tra(280_000L,
                new BanHangService.GiaoHangRequest("Nguyễn Thị Lan", "0901 000 001", "Số 15 ngõ 120 Trần Phú, Hà Đông, Hà Nội"), null));
        assertEquals(280_000L, kq.get("thanhTien"), "250.000 + 30.000 phí giao hàng");
        HoaDon h = m.hoaDons.stream().filter(x -> x.getId().equals(id)).findFirst().orElseThrow();
        assertEquals(HoaDon.GIAO_HANG, h.getLoaiDon());
        assertEquals(HoaDon.CHO_LAY_HANG, h.getTrangThai(), "đã trả tiền tại quầy nên bỏ qua bước chờ xác nhận");
        assertEquals("0901000001", h.getSdtNguoiNhan());
        assertTrue(lichSus.stream().anyMatch(l -> "Chờ giao hàng".equals(l.getHanhDong())));
    }

    @Test
    void ketCa_khiConHoaDonCho_bichan() {
        Integer id = taoHoaDon();
        service.themSanPham(id, new HoaDonChoService.ThemSanPhamRequest(cau.getId(), 1));
        GiaoCaService giaoCa = new GiaoCaService(m.giaoCaRepo(), m.nhanVienRepo(), m.lichRepo(), hoaDonRepo(), thanhToanRepo(), m.phuongThucRepo());
        ApiException e = loi(() -> giaoCa.ketCa(new GiaoCaService.KetCaRequest(BigDecimal.ZERO, BigDecimal.ZERO, null, null)));
        assertEquals("CON_HOA_DON_CHO", e.getCode());
        assertEquals(0, giaoCa.tongKet(m.giaoCas.get(m.giaoCas.size() - 1)).get("soHoaDon"), "hóa đơn chờ chưa tính vào tổng kết ca");
    }

    @Test
    void locSanPham_moiNhatTruoc_anHetHang_locDanhMucVaKhoangGia() {
        Map<String, Object> kq = banHang.locSanPham(new BanHangService.LocSanPham(null, null, null, null, null, null, null, null, null, null));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ds = (List<Map<String, Object>>) kq.get("items");
        assertEquals(List.of("SP07-TRANG", "SP01-DEN-4U-G5"), ds.stream().map(x -> x.get("ma")).toList(), "mới nhất trước, bỏ sản phẩm hết hàng");
        assertEquals(2, kq.get("tongSo"));
        kq = banHang.locSanPham(new BanHangService.LocSanPham(null, 2, null, null, null, null, null, null, null, null));
        assertEquals(1, kq.get("tongSo"));
        kq = banHang.locSanPham(new BanHangService.LocSanPham(null, null, null, null, null, null, 1_000_000L, 5_000_000L, null, null));
        assertEquals(1, kq.get("tongSo"));
        assertEquals(HttpStatus.BAD_REQUEST,
                loi(() -> banHang.locSanPham(new BanHangService.LocSanPham(null, null, null, null, null, null, 9L, 1L, null, null))).getStatus(),
                "giá từ lớn hơn giá đến");
        kq = banHang.locSanPham(new BanHangService.LocSanPham(null, null, null, null, null, null, null, null, 0, 1));
        assertEquals(2, kq.get("soTrang"));
    }
}
