package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.KhoGia;
import com.footstyle.demo.entity.ChuVi;
import com.footstyle.demo.entity.GiaoCa;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.HoaDonChiTiet;
import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.entity.LichSuHoaDon;
import com.footstyle.demo.entity.LichSuThanhToan;
import com.footstyle.demo.entity.MauSac;
import com.footstyle.demo.entity.PhieuGiamGia;
import com.footstyle.demo.entity.PhieuGiamGiaKhachHang;
import com.footstyle.demo.entity.SanPham;
import com.footstyle.demo.entity.SanPhamChiTiet;
import com.footstyle.demo.entity.ThuongHieu;
import com.footstyle.demo.entity.TrongLuong;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class BanHangServiceTest {

    private final CaLamMau m = new CaLamMau();
    private final List<SanPhamChiTiet> bienThes = new ArrayList<>();
    private final List<KhachHang> khachs = new ArrayList<>();
    private final List<PhieuGiamGia> phieus = new ArrayList<>();
    private final List<PhieuGiamGiaKhachHang> phieuKhachs = new ArrayList<>();
    private final List<HoaDonChiTiet> chiTiets = new ArrayList<>();
    private final List<LichSuHoaDon> lichSus = new ArrayList<>();
    private final Map<Long, Double> phanTramDot = new HashMap<>();
    private final BanHangService service;
    private final SanPhamChiTiet vot;   // 4.290.000, tồn 5, đợt 10%
    private final SanPhamChiTiet cau;   // 250.000, tồn 50
    private final SanPhamChiTiet ngung; // biến thể ngừng bán
    private final KhachHang lan;
    private final KhachHang hung;
    private static final LocalDateTime LUC = CaLamMau.HOM_NAY.atTime(9, 0);

    BanHangServiceTest() {
        SanPham sp1 = sanPham(1, "SP01", "Vợt Yonex Astrox 99 Pro", "Yonex", 1);
        SanPham sp2 = sanPham(2, "SP07", "Ống cầu lông Thành Công", "Thành Công", 1);
        vot = bienThe(1, sp1, "SP01-DEN-4U-G5", "Đen", "4U", "G5", 4_290_000L, 5, 1);
        cau = bienThe(2, sp2, "SP07-TRANG", "Trắng", "Không áp dụng", "Không áp dụng", 250_000L, 50, 1);
        ngung = bienThe(3, sp1, "SP01-DO-3U-G5", "Đỏ", "3U", "G5", 4_290_000L, 3, 0);
        bienThe(4, sp1, "SP01-VANG-4U-G6", "Vàng", "4U", "G6", 4_290_000L, 0, 1);
        phanTramDot.put(1L, 10.0);
        lan = khach(1, "KH01", "Nguyễn Thị Lan", "0901000001");
        hung = khach(2, "KH02", "Trần Văn Hùng", "0901000002");

        GiamGiaApDungService giam = new GiamGiaApDungService(null) {
            @Override
            public Map<Long, Double> phanTramTheoBienThe(LocalDateTime thoiDiem) {
                return phanTramDot;
            }
        };
        GiaoCaService giaoCa = new GiaoCaService(m.giaoCaRepo(), m.nhanVienRepo(), m.lichRepo(), m.hoaDonRepo(), m.thanhToanRepo(), m.phuongThucRepo());
        service = new BanHangService(spctRepo(), KhoGia.tao(HinhAnhSanPhamRepository.class, (t, a) -> "findByTrangThaiOrderByLaAnhChinhDescIdAsc".equals(t) ? List.of() : KhoGia.KHONG_CO),
                khachRepo(), m.nhanVienRepo(), phieuRepo(), phieuKhachRepo(), hoaDonRepo(),
                KhoGia.tao(HoaDonChiTietRepository.class, (t, a) -> {
                    if ("save".equals(t)) {
                        chiTiets.add((HoaDonChiTiet) a[0]);
                        return a[0];
                    }
                    return KhoGia.KHONG_CO;
                }),
                KhoGia.tao(LichSuHoaDonRepository.class, (t, a) -> {
                    if ("save".equals(t)) {
                        lichSus.add((LichSuHoaDon) a[0]);
                        return a[0];
                    }
                    return KhoGia.KHONG_CO;
                }),
                thanhToanRepo(), m.phuongThucRepo(), giam, giaoCa);
        service.datDongHo(java.time.Clock.fixed(LUC.atZone(CaLamMau.VN).toInstant(), CaLamMau.VN));
    }

    @AfterEach
    void don() {
        XacThuc.xoa();
    }

    /* ---------- dữ liệu mẫu ---------- */

    private SanPham sanPham(int id, String ma, String ten, String thuongHieu, int tt) {
        SanPham sp = new SanPham();
        sp.setId(id);
        sp.setMaSanPham(ma);
        sp.setTenSanPham(ten);
        ThuongHieu th = new ThuongHieu();
        th.setTen(thuongHieu);
        sp.setThuongHieu(th);
        sp.setTrangThai(tt);
        return sp;
    }

    private SanPhamChiTiet bienThe(int id, SanPham sp, String ma, String mau, String tl, String cv, long gia, int ton, int tt) {
        SanPhamChiTiet c = new SanPhamChiTiet();
        c.setId(id);
        c.setSanPham(sp);
        c.setMaSpct(ma);
        MauSac ms = new MauSac();
        ms.setId(id);
        ms.setTen(mau);
        c.setMauSac(ms);
        TrongLuong t = new TrongLuong();
        t.setTen(tl);
        c.setTrongLuong(t);
        ChuVi v = new ChuVi();
        v.setTen(cv);
        c.setChuVi(v);
        c.setGiaBan(gia);
        c.setSoLuongTon(ton);
        c.setTrangThai(tt);
        bienThes.add(c);
        return c;
    }

    private KhachHang khach(int id, String ma, String ten, String sdt) {
        KhachHang k = new KhachHang();
        k.setId(id);
        k.setMaKhachHang(ma);
        k.setHoTen(ten);
        k.setSdt(sdt);
        k.setEmail(ma.toLowerCase() + "@gmail.com");
        k.setTrangThai(1);
        khachs.add(k);
        return k;
    }

    private PhieuGiamGia phieu(String ma, int doiTuong, int loai, long giaTri, Long toiDa, long donMin, Integer soLuong, int daDung, Integer gioiHan) {
        PhieuGiamGia p = new PhieuGiamGia();
        p.setId(phieus.size() + 1);
        p.setMaPhieu(ma);
        p.setTenPhieu("Phiếu " + ma);
        p.setDoiTuongApDung(doiTuong);
        p.setLoaiGiam(loai);
        p.setGiaTriGiam(giaTri);
        p.setGiamToiDa(toiDa);
        p.setGiaTriDonHangToiThieu(donMin);
        p.setSoLuong(soLuong);
        p.setSoLuongDaDung(daDung);
        p.setGioiHanMoiKhach(gioiHan);
        p.setNgayBatDau(LUC.minusDays(5));
        p.setNgayKetThuc(LUC.plusDays(5));
        p.setTrangThai(1);
        phieus.add(p);
        return p;
    }

    private void tangPhieu(PhieuGiamGia p, KhachHang k) {
        PhieuGiamGiaKhachHang x = new PhieuGiamGiaKhachHang();
        x.setId(phieuKhachs.size() + 1);
        x.setIdKhachHang(k.getId());
        x.setIdPhieuGiamGia(p.getId());
        x.setTrangThai(1);
        phieuKhachs.add(x);
    }

    /* ---------- kho giả ---------- */

    private SanPhamChiTietRepository spctRepo() {
        return KhoGia.tao(SanPhamChiTietRepository.class, (t, a) -> switch (t) {
            case "findAll" -> new ArrayList<>(bienThes);
            case "findById" -> bienThes.stream().filter(c -> c.getId().equals(a[0])).findFirst();
            case "findFirstByMaSpctIgnoreCase" -> bienThes.stream().filter(c -> c.getMaSpct().equalsIgnoreCase((String) a[0])).findFirst();
            case "truTonKho" -> {
                SanPhamChiTiet c = bienThes.stream().filter(x -> x.getId().equals(a[0])).findFirst().orElseThrow();
                int sl = (Integer) a[1];
                if (c.getSoLuongTon() < sl) yield 0;
                c.setSoLuongTon(c.getSoLuongTon() - sl);
                yield 1;
            }
            default -> KhoGia.KHONG_CO;
        });
    }

    private KhachHangRepository khachRepo() {
        return KhoGia.tao(KhachHangRepository.class, (t, a) -> switch (t) {
            case "findById" -> khachs.stream().filter(k -> k.getId().equals(a[0])).findFirst();
            case "findByTrangThaiOrderByHoTenAsc" -> khachs.stream().filter(k -> k.getTrangThai().equals(a[0])).toList();
            default -> KhoGia.KHONG_CO;
        });
    }

    private PhieuGiamGiaRepository phieuRepo() {
        return KhoGia.tao(PhieuGiamGiaRepository.class, (t, a) -> switch (t) {
            case "findAll" -> new ArrayList<>(phieus);
            case "findFirstByMaPhieuIgnoreCase" -> phieus.stream().filter(p -> p.getMaPhieu().equalsIgnoreCase((String) a[0])).findFirst();
            case "tangLuotDung" -> {
                PhieuGiamGia p = phieus.stream().filter(x -> x.getId().equals(a[0])).findFirst().orElseThrow();
                int da = p.getSoLuongDaDung() == null ? 0 : p.getSoLuongDaDung();
                if (p.getSoLuong() != null && da >= p.getSoLuong()) yield 0;
                p.setSoLuongDaDung(da + 1);
                yield 1;
            }
            default -> KhoGia.KHONG_CO;
        });
    }

    private PhieuGiamGiaKhachHangRepository phieuKhachRepo() {
        return KhoGia.tao(PhieuGiamGiaKhachHangRepository.class, (t, a) -> switch (t) {
            case "findByIdKhachHang" -> phieuKhachs.stream().filter(x -> x.getIdKhachHang().equals(a[0])).toList();
            case "save" -> a[0];
            default -> KhoGia.KHONG_CO;
        });
    }

    private HoaDonRepository hoaDonRepo() {
        return KhoGia.tao(HoaDonRepository.class, (t, a) -> switch (t) {
            case "findAllMaHoaDon" -> m.hoaDons.stream().map(HoaDon::getMaHoaDon).toList();
            case "countByKhachHang_IdAndPhieuGiamGia_IdAndTrangThaiNot" -> m.hoaDons.stream()
                    .filter(h -> h.getKhachHang() != null && h.getKhachHang().getId().equals(a[0]))
                    .filter(h -> h.getPhieuGiamGia() != null && h.getPhieuGiamGia().getId().equals(a[1]))
                    .filter(h -> !Objects.equals(h.getTrangThai(), a[2])).count();
            case "saveAndFlush", "save" -> {
                HoaDon h = (HoaDon) a[0];
                if (h.getId() == null) {
                    h.setId(m.hoaDons.size() + 100);
                    m.hoaDons.add(h);
                }
                yield h;
            }
            case "findByIdGiaoCaOrderByNgayTaoAsc" -> m.hoaDons.stream().filter(h -> Objects.equals(h.getIdGiaoCa(), a[0])).toList();
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

    private GiaoCa moCa() {
        GiaoCa g = new GiaoCa();
        g.setId(7);
        g.setMaGiaoCa("GC07");
        g.setNhanVien(m.bao);
        g.setThoiGianNhanCa(LUC.minusHours(2));
        g.setTienDauCa(BigDecimal.ZERO);
        g.setTrangThai(GiaoCa.DANG_TRONG_CA);
        m.giaoCas.add(g);
        CaLamMau.dangNhap(m.bao);
        return g;
    }

    private static BanHangService.DongHang dong(SanPhamChiTiet c, int sl, Long gia) {
        return new BanHangService.DongHang(c.getId(), sl, gia);
    }

    private static ApiException loi(Runnable r) {
        return assertThrows(ApiException.class, r::run);
    }

    private BanHangService.ThanhToanRequest don(Integer idKhach, String phieu, long tienMat, long ck, BanHangService.DongHang... dongs) {
        return new BanHangService.ThanhToanRequest(idKhach, phieu, null, List.of(dongs), tienMat, ck, null, null);
    }

    /* ---------- tìm sản phẩm ---------- */

    @Test
    void timSanPham_khongDau_boNgungBan_hetHangXepCuoi_giaDaTruDot() {
        List<Map<String, Object>> ds = service.timSanPham("vot yonex", null);
        assertEquals(List.of("SP01-DEN-4U-G5", "SP01-VANG-4U-G6"), ds.stream().map(x -> x.get("ma")).toList(), "bỏ biến thể ngừng bán, hết hàng xếp cuối");
        Map<String, Object> v = ds.get(0);
        assertEquals(4_290_000L, v.get("giaGoc"));
        assertEquals(3_861_000L, v.get("donGia"));
        assertEquals("Đen · 4U · G5", v.get("phanLoai"));
        assertEquals("Trắng", service.timSanPham("thanh cong", null).get(0).get("phanLoai"), "bỏ thuộc tính Không áp dụng");
        assertEquals(3, service.timSanPham("", null).size());
    }

    @Test
    void timTheoMa_khongPhanBietHoaThuong() {
        assertEquals(1, service.timTheoMa(" sp01-den-4u-g5 ").get("id"));
        assertEquals(HttpStatus.NOT_FOUND, loi(() -> service.timTheoMa("KHONG-CO")).getStatus());
        assertEquals(HttpStatus.CONFLICT, loi(() -> service.timTheoMa("SP01-DO-3U-G5")).getStatus());
    }

    @Test
    void timKhach_theoTenKhongDauHoacSdt() {
        assertEquals("KH01", service.timKhach("nguyen thi lan").get(0).get("ma"));
        assertEquals("KH02", service.timKhach("0901000002").get(0).get("ma"));
        assertTrue(service.timKhach("  ").isEmpty());
    }

    /* ---------- thanh toán ---------- */

    @Test
    void thanhToanTienMat_taoHoaDonHoanThanh_truKho_traTienThua() {
        GiaoCa ca = moCa();
        PhieuGiamGia p = phieu("GIAM10", 1, 1, 10, 200_000L, 1_000_000, 100, 3, null);
        Map<String, Object> kq = service.thanhToan(don(null, "giam10", 4_500_000, 0, dong(vot, 1, 3_861_000L), dong(cau, 2, 250_000L)));
        // tổng 3.861.000 + 500.000 = 4.361.000; giảm 10% = 436.100 nhưng tối đa 200.000
        assertEquals(4_361_000L, kq.get("tongTien"));
        assertEquals(200_000L, kq.get("soTienGiam"));
        assertEquals(4_161_000L, kq.get("thanhTien"));
        assertEquals(339_000L, kq.get("tienThua"));
        HoaDon hd = m.hoaDons.get(0);
        assertEquals("HD01", hd.getMaHoaDon());
        assertEquals(HoaDon.TAI_QUAY, hd.getLoaiDon());
        assertEquals(HoaDon.HOAN_THANH, hd.getTrangThai());
        assertEquals(ca.getId(), hd.getIdGiaoCa());
        assertEquals(m.bao, hd.getNhanVien());
        assertNull(hd.getKhachHang(), "khách lẻ");
        assertEquals(1, hd.getIdPhuongThucThanhToan(), "PT01 tiền mặt");
        assertEquals(4, vot.getSoLuongTon());
        assertEquals(48, cau.getSoLuongTon());
        assertEquals(4, p.getSoLuongDaDung());
        assertEquals(2, chiTiets.size());
        assertEquals(new BigDecimal("3861000"), chiTiets.get(0).getDonGia());
        assertEquals(List.of("Tạo đơn hàng", "Đã hoàn thành"), lichSus.stream().map(LichSuHoaDon::getHanhDong).toList());
        assertEquals("Lê Quốc Bảo", lichSus.get(0).getNguoiThaoTac());
        assertEquals(1, m.thanhToans.size());
        assertEquals(new BigDecimal("4161000"), m.thanhToans.get(0).getSoTien(), "chỉ ghi số tiền thực thu, không tính tiền thừa");
        assertTrue(m.thanhToans.get(0).getMoTa().contains("trả lại 339.000 ₫"));
    }

    @Test
    void thanhToanKetHop_vaChuyenKhoanDu() {
        moCa();
        service.thanhToan(new BanHangService.ThanhToanRequest(1, null, " Khách quen ", List.of(dong(cau, 4, 250_000L)), 800_000L, 300_000L, "FT2610", null));
        HoaDon hd = m.hoaDons.get(0);
        assertEquals(5, hd.getIdPhuongThucThanhToan(), "PT05 kết hợp");
        assertEquals("Khách quen", hd.getGhiChu());
        assertEquals(lan, hd.getKhachHang());
        assertEquals(2, m.thanhToans.size());
        assertEquals(new BigDecimal("700000"), m.thanhToans.get(0).getSoTien());
        assertEquals(new BigDecimal("300000"), m.thanhToans.get(1).getSoTien());
        assertEquals("FT2610", m.thanhToans.get(1).getMaGiaoDich());
        assertEquals(2, m.thanhToans.get(1).getIdPhuongThucThanhToan());
        service.thanhToan(don(null, null, 0, 250_000, dong(cau, 1, 250_000L)));
        assertEquals(2, m.hoaDons.get(1).getIdPhuongThucThanhToan(), "PT02 chuyển khoản");
        assertEquals("HD02", m.hoaDons.get(1).getMaHoaDon());
        // tổng kết ca tách đúng tiền mặt / chuyển khoản
        Map<String, Object> tk = new GiaoCaService(m.giaoCaRepo(), m.nhanVienRepo(), m.lichRepo(), m.hoaDonRepo(), m.thanhToanRepo(),
                m.phuongThucRepo()).tongKet(m.giaoCas.get(0));
        assertEquals(new BigDecimal("700000"), tk.get("tienMat"));
        assertEquals(new BigDecimal("550000"), tk.get("chuyenKhoan"));
    }

    @Test
    void tienKhongDu_hoacChuyenKhoanQuaSo() {
        moCa();
        assertTrue(loi(() -> service.thanhToan(don(null, null, 200_000, 0, dong(cau, 1, 250_000L)))).getMessage().contains("còn thiếu 50.000 ₫"));
        assertTrue(loi(() -> service.thanhToan(don(null, null, 0, 300_000, dong(cau, 1, 250_000L)))).getMessage().contains("không được lớn hơn"));
        assertEquals(HttpStatus.BAD_REQUEST, loi(() -> service.thanhToan(don(null, null, -1, 0, dong(cau, 1, 250_000L)))).getStatus());
        assertEquals(50, cau.getSoLuongTon(), "lỗi thì không trừ kho");
    }

    @Test
    void chuaNhanCa_khongBanDuoc() {
        CaLamMau.dangNhap(m.bao);
        ApiException e = loi(() -> service.thanhToan(don(null, null, 250_000, 0, dong(cau, 1, 250_000L))));
        assertEquals("CHUA_NHAN_CA", e.getCode());
    }

    @Test
    void giaDoiSoVoiGiaoDien_baoDeXacNhan_xacNhanThiTinhGiaMoi() {
        moCa();
        ApiException e = loi(() -> service.thanhToan(don(null, null, 5_000_000, 0, dong(vot, 1, 4_290_000L))));
        assertEquals("GIA_THAY_DOI", e.getCode());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) e.getChiTiet().get("items");
        assertEquals(3_861_000L, items.get(0).get("donGiaMoi"));
        Map<String, Object> kq = service.thanhToan(new BanHangService.ThanhToanRequest(null, null, null,
                List.of(dong(vot, 1, 4_290_000L)), 5_000_000L, 0L, null, true));
        assertEquals(3_861_000L, kq.get("thanhTien"));
    }

    @Test
    void khongDuHang_vaSoLuongSai_vaNgungBan() {
        moCa();
        ApiException e = loi(() -> service.thanhToan(don(null, null, 30_000_000, 0, dong(vot, 6, 3_861_000L))));
        assertEquals("HET_HANG", e.getCode());
        assertTrue(e.getMessage().contains("còn 5"));
        assertEquals(HttpStatus.BAD_REQUEST, loi(() -> service.thanhToan(don(null, null, 0, 0, dong(cau, 0, 250_000L)))).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, loi(() -> service.thanhToan(don(null, null, 0, 0, dong(cau, 1000, 250_000L)))).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, loi(() -> service.thanhToan(don(null, null, 0, 0))).getStatus());
        assertEquals(HttpStatus.CONFLICT, loi(() -> service.thanhToan(don(null, null, 9_000_000, 0, dong(ngung, 1, 4_290_000L)))).getStatus());
    }

    @Test
    void dongTrungBienThe_duocGop() {
        moCa();
        service.thanhToan(don(null, null, 750_000, 0, dong(cau, 1, 250_000L), dong(cau, 2, 250_000L)));
        assertEquals(1, chiTiets.size());
        assertEquals(3, chiTiets.get(0).getSoLuong());
        assertEquals(47, cau.getSoLuongTon());
    }

    @Test
    void phieuCaNhan_chiKhachDuocTang_dungXongDanhDauDaDung() {
        moCa();
        PhieuGiamGia p = phieu("TRIAN50", 2, 2, 50_000, null, 300_000, 3, 0, 1);
        tangPhieu(p, lan);
        assertTrue(loi(() -> service.thanhToan(don(null, "TRIAN50", 500_000, 0, dong(cau, 2, 250_000L)))).getMessage().contains("chọn khách hàng"));
        assertTrue(loi(() -> service.thanhToan(don(2, "TRIAN50", 500_000, 0, dong(cau, 2, 250_000L)))).getMessage().contains("không dành cho khách"));
        Map<String, Object> kq = service.thanhToan(don(1, "TRIAN50", 450_000, 0, dong(cau, 2, 250_000L)));
        assertEquals(50_000L, kq.get("soTienGiam"));
        assertNotNull(phieuKhachs.get(0).getNgaySuDung());
        assertTrue(loi(() -> service.thanhToan(don(1, "TRIAN50", 500_000, 0, dong(cau, 2, 250_000L)))).getMessage().contains("đã dùng phiếu"));
    }

    @Test
    void phieuKhongDuDieuKien() {
        moCa();
        phieu("DONTO", 1, 2, 30_000, null, 600_000, 100, 0, null);
        phieu("HETLUOT", 1, 2, 30_000, null, 0, 5, 5, null);
        PhieuGiamGia hetHan = phieu("CU", 1, 2, 30_000, null, 0, 100, 0, null);
        hetHan.setNgayKetThuc(LUC.minusDays(1));
        phieu("MOTLAN", 1, 2, 30_000, null, 0, 100, 0, 1);
        assertTrue(loi(() -> service.thanhToan(don(null, "DONTO", 500_000, 0, dong(cau, 2, 250_000L)))).getMessage().contains("còn thiếu 100.000 ₫"));
        assertTrue(loi(() -> service.thanhToan(don(null, "HETLUOT", 500_000, 0, dong(cau, 2, 250_000L)))).getMessage().contains("hết lượt"));
        assertTrue(loi(() -> service.thanhToan(don(null, "CU", 500_000, 0, dong(cau, 2, 250_000L)))).getMessage().contains("hết hạn"));
        assertTrue(loi(() -> service.thanhToan(don(null, "KHONGCO", 500_000, 0, dong(cau, 2, 250_000L)))).getMessage().contains("Không tìm thấy"));
        service.thanhToan(don(1, "MOTLAN", 500_000, 0, dong(cau, 2, 250_000L)));
        assertTrue(loi(() -> service.thanhToan(don(1, "MOTLAN", 500_000, 0, dong(cau, 2, 250_000L)))).getMessage().contains("tối đa 1 lần"));
        // khách lẻ không bị giới hạn theo khách
        service.thanhToan(don(null, "MOTLAN", 500_000, 0, dong(cau, 2, 250_000L)));
    }

    @Test
    void danhSachPhieuChoDon_dungDuocTruoc_giamNhieuLenDau_phieuCaNhanChiCuaKhachDo() {
        phieu("GIAM10", 1, 1, 10, 200_000L, 1_000_000, 100, 0, null);
        phieu("GIAM50K", 1, 2, 50_000, null, 300_000, 100, 0, null);
        phieu("TO5TR", 1, 2, 500_000, null, 5_000_000, 100, 0, null);
        PhieuGiamGia rieng = phieu("RIENG", 2, 2, 80_000, null, 0, 1, 0, 1);
        tangPhieu(rieng, lan);
        PhieuGiamGia tat = phieu("TAT", 1, 2, 10_000, null, 0, 100, 0, null);
        tat.setTrangThai(0);
        List<Map<String, Object>> khachLe = service.phieuChoDon(null, 1_500_000L);
        assertEquals(List.of("GIAM10", "GIAM50K", "TO5TR"), khachLe.stream().map(x -> x.get("ma")).toList());
        assertEquals(150_000L, khachLe.get(0).get("soTienGiam"));
        assertEquals(false, khachLe.get(2).get("apDungDuoc"));
        assertTrue(((String) khachLe.get(2).get("lyDo")).contains("còn thiếu"));
        List<Map<String, Object>> choLan = service.phieuChoDon(1, 1_500_000L);
        assertEquals(List.of("GIAM10", "RIENG", "GIAM50K", "TO5TR"), choLan.stream().map(x -> x.get("ma")).toList());
        assertEquals(true, choLan.get(1).get("caNhan"));
        assertFalse(service.phieuChoDon(2, 1_500_000L).stream().anyMatch(x -> "RIENG".equals(x.get("ma"))));
    }

    @Test
    void cachTinhGiamVaGia_khopGiaoDien() {
        PhieuGiamGia pt = new PhieuGiamGia();
        pt.setLoaiGiam(PhieuGiamGia.LOAI_PHAN_TRAM);
        pt.setGiaTriGiam(15L);
        assertEquals(149_998L, PhieuApDung.soTienGiam(pt, 999_990L), "làm tròn xuống");
        pt.setGiamToiDa(100_000L);
        assertEquals(100_000L, PhieuApDung.soTienGiam(pt, 999_990L));
        PhieuGiamGia tien = new PhieuGiamGia();
        tien.setLoaiGiam(PhieuGiamGia.LOAI_SO_TIEN);
        tien.setGiaTriGiam(300_000L);
        assertEquals(250_000L, PhieuApDung.soTienGiam(tien, 250_000L), "không giảm quá tổng đơn");
        assertEquals(2_187_500L, PhieuApDung.giaSauDot(2_500_000L, 12.5));
        assertEquals(2_125_000L, PhieuApDung.giaSauDot(2_500_000L, 15));
        assertEquals(0L, PhieuApDung.giaSauDot(2_500_000L, 100));
        assertEquals(2_500_000L, PhieuApDung.giaSauDot(2_500_000L, 0));
    }
}
