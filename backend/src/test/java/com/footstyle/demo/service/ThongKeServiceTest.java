package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.KhoGia;
import com.footstyle.demo.entity.GiaoCa;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.HoaDonChiTiet;
import com.footstyle.demo.entity.LichLamViec;
import com.footstyle.demo.entity.LichSuThanhToan;
import com.footstyle.demo.entity.MauSac;
import com.footstyle.demo.entity.NhanVien;
import com.footstyle.demo.entity.SanPham;
import com.footstyle.demo.entity.SanPhamChiTiet;
import com.footstyle.demo.entity.ThuongHieu;
import com.footstyle.demo.entity.TrongLuong;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.HoaDonChiTietRepository;
import com.footstyle.demo.repository.HoaDonRepository;
import com.footstyle.demo.repository.LichSuThanhToanRepository;
import com.footstyle.demo.repository.SanPhamChiTietRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ThongKeServiceTest {

    private final CaLamMau m = new CaLamMau();
    private final List<HoaDonChiTiet> chiTiets = new ArrayList<>();
    private final List<SanPhamChiTiet> bienThes = new ArrayList<>();
    private final ThongKeService service;
    private static final LocalDate HOM_NAY = CaLamMau.HOM_NAY; // thứ 6 16/10/2026

    ThongKeServiceTest() {
        service = new ThongKeService(
                KhoGia.tao(HoaDonRepository.class, (t, a) -> "findByTrangThaiAndNgayTaoBetween".equals(t)
                        ? m.hoaDons.stream().filter(h -> h.getTrangThai().equals(a[0]) && trong(h.getNgayTao(), a[1], a[2])).toList()
                        : KhoGia.KHONG_CO),
                KhoGia.tao(HoaDonChiTietRepository.class, (t, a) -> "findByHoaDon_TrangThaiAndHoaDon_NgayTaoBetween".equals(t)
                        ? chiTiets.stream().filter(c -> c.getHoaDon().getTrangThai().equals(a[0]) && trong(c.getHoaDon().getNgayTao(), a[1], a[2])).toList()
                        : KhoGia.KHONG_CO),
                KhoGia.tao(LichSuThanhToanRepository.class, (t, a) -> "findByHoaDon_TrangThaiAndHoaDon_NgayTaoBetween".equals(t)
                        ? m.thanhToans.stream().filter(x -> x.getHoaDon().getTrangThai().equals(a[0]) && trong(x.getHoaDon().getNgayTao(), a[1], a[2])).toList()
                        : KhoGia.KHONG_CO),
                KhoGia.tao(SanPhamChiTietRepository.class, (t, a) -> "findByTrangThaiAndSoLuongTonLessThanEqualOrderBySoLuongTonAscIdAsc".equals(t)
                        ? bienThes.stream().filter(c -> c.getTrangThai().equals(a[0]) && c.getSoLuongTon() <= (Integer) a[1])
                                .sorted(java.util.Comparator.comparing(SanPhamChiTiet::getSoLuongTon)).toList()
                        : KhoGia.KHONG_CO),
                m.phuongThucRepo(), m.caRepo(), m.lichRepo(), m.giaoCaRepo());
        service.datDongHo(CaLamMau.luc(HOM_NAY, "13:00"));
    }

    private static boolean trong(LocalDateTime t, Object tu, Object den) {
        return t != null && !t.isBefore((LocalDateTime) tu) && !t.isAfter((LocalDateTime) den);
    }

    private SanPhamChiTiet bienThe(int id, SanPham sp, String mau, String tl, int ton, int tt) {
        SanPhamChiTiet c = new SanPhamChiTiet();
        c.setId(id);
        c.setSanPham(sp);
        c.setMaSpct("SPCT" + id);
        MauSac ms = new MauSac();
        ms.setTen(mau);
        c.setMauSac(ms);
        TrongLuong t = new TrongLuong();
        t.setTen(tl);
        c.setTrongLuong(t);
        c.setSoLuongTon(ton);
        c.setTrangThai(tt);
        bienThes.add(c);
        return c;
    }

    private static SanPham sanPham(int id, String ten, int tt) {
        SanPham sp = new SanPham();
        sp.setId(id);
        sp.setMaSanPham("SP0" + id);
        sp.setTenSanPham(ten);
        ThuongHieu th = new ThuongHieu();
        th.setTen("Yonex");
        sp.setThuongHieu(th);
        sp.setTrangThai(tt);
        return sp;
    }

    private void ban(HoaDon h, SanPhamChiTiet c, int sl, long donGia) {
        HoaDonChiTiet ct = new HoaDonChiTiet();
        ct.setHoaDon(h);
        ct.setSanPhamChiTiet(c);
        ct.setSoLuong(sl);
        ct.setDonGia(BigDecimal.valueOf(donGia));
        ct.setThanhTien(BigDecimal.valueOf(donGia * sl));
        chiTiets.add(ct);
    }

    private GiaoCa giaoCa(int id, NhanVien nv, LichLamViec lich, LocalDateTime nhan, LocalDateTime ket, int muon, Long chenhLech) {
        GiaoCa g = new GiaoCa();
        g.setId(id);
        g.setMaGiaoCa("GC0" + id);
        g.setNhanVien(nv);
        g.setLichLamViec(lich);
        g.setThoiGianNhanCa(nhan);
        g.setThoiGianKetCa(ket);
        g.setSoPhutDiMuon(muon);
        g.setTrangThai(ket == null ? GiaoCa.DANG_TRONG_CA : GiaoCa.DA_GIAO_CA);
        g.setChenhLech(chenhLech == null ? null : BigDecimal.valueOf(chenhLech));
        g.setGhiChuKetCa(chenhLech != null && chenhLech != 0 ? "Thối nhầm" : null);
        m.giaoCas.add(g);
        return g;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> ds(Map<String, Object> kq, String khoa) {
        return (List<Map<String, Object>>) kq.get(khoa);
    }

    @Test
    void khoangNgay_macDinh30Ngay_vaKiemTra() {
        Map<String, Object> kq = service.tongHop(null, null, null);
        assertEquals(HOM_NAY, kq.get("denNgay"));
        assertEquals(HOM_NAY.minusDays(29), kq.get("tuNgay"));
        assertEquals(5, kq.get("nguongSapHet"));
        assertEquals("Từ ngày phải trước hoặc cùng ngày với Đến ngày.",
                assertThrows(ApiException.class, () -> service.tongHop(HOM_NAY, HOM_NAY.minusDays(1), null)).getMessage());
        assertThrows(ApiException.class, () -> service.tongHop(HOM_NAY.minusDays(400), HOM_NAY, null));
        assertThrows(ApiException.class, () -> service.tongHop(null, null, -1));
        assertThrows(ApiException.class, () -> service.tongHop(null, null, 101));
    }

    @Test
    void sanPhamBanChay_chiTinhDonHoanThanhTrongKhoang() {
        SanPham astrox = sanPham(1, "Yonex Astrox 99", 1);
        SanPham cau = sanPham(2, "Cầu lông Thành Công", 1);
        SanPhamChiTiet den4u = bienThe(1, astrox, "Đen", "4U", 20, 1);
        SanPhamChiTiet do3u = bienThe(2, astrox, "Đỏ", "3U", 20, 1);
        SanPhamChiTiet ong = bienThe(3, cau, "Trắng", "Không áp dụng", 50, 1);
        HoaDon h1 = m.hoaDon(null, 0, HoaDon.HOAN_THANH);
        HoaDon h2 = m.hoaDon(null, 0, HoaDon.HOAN_THANH);
        HoaDon huy = m.hoaDon(null, 0, HoaDon.DA_HUY);
        HoaDon cu = m.hoaDon(null, 0, HoaDon.HOAN_THANH);
        cu.setNgayTao(HOM_NAY.minusDays(60).atTime(9, 0));
        ban(h1, den4u, 1, 4_000_000);
        ban(h2, den4u, 1, 4_000_000);
        ban(h2, do3u, 2, 4_100_000);
        ban(h1, ong, 3, 250_000);
        ban(huy, ong, 10, 250_000); // đơn hủy: không tính
        ban(cu, ong, 10, 250_000);  // ngoài khoảng: không tính
        List<Map<String, Object>> top = ds(service.tongHop(null, null, null), "sanPhamBanChay");
        assertEquals(2, top.size());
        assertEquals("Yonex Astrox 99", top.get(0).get("tenSanPham")); // 4 cây > 3 ống
        assertEquals(4L, top.get(0).get("soLuong"));
        assertEquals(16_200_000L, top.get(0).get("doanhThu"));
        assertEquals(2, top.get(0).get("soDon"));
        assertEquals("Đen · 4U", top.get(0).get("phanLoaiBanChay"), "Đen 4U và Đỏ 3U cùng 2 cây: lấy biến thể có trước");
        assertEquals(3L, top.get(1).get("soLuong"));
        assertEquals("Trắng", top.get(1).get("phanLoaiBanChay"));
    }

    @Test
    void sapHetHang_chiBienTheDangBan_itNhatLenTruoc() {
        SanPham sp = sanPham(1, "Yonex Astrox 99", 1);
        SanPham ngung = sanPham(2, "Mẫu cũ", 0);
        bienThe(1, sp, "Đen", "4U", 3, 1);
        bienThe(2, sp, "Đỏ", "3U", 0, 1);
        bienThe(3, sp, "Vàng", "4U", 5, 1);
        bienThe(4, sp, "Xanh", "4U", 6, 1);
        bienThe(5, sp, "Trắng", "4U", 1, 0);  // biến thể ngừng bán
        bienThe(6, ngung, "Đen", "4U", 1, 1); // sản phẩm ngừng bán
        Map<String, Object> kq = service.tongHop(null, null, null);
        assertEquals(3, kq.get("soSapHet"));
        assertEquals(1L, kq.get("soHetHang"));
        assertEquals(List.of(0, 3, 5), ds(kq, "sapHetHang").stream().map(x -> x.get("tonKho")).toList());
        assertEquals("Đỏ · 3U", ds(kq, "sapHetHang").get(0).get("phanLoai"));
        assertEquals(1, service.tongHop(null, null, 0).get("soSapHet"));
    }

    @Test
    void theoPhuongThuc_tachDonKetHop_vaDonCuChuaGhiPhuongThuc() {
        m.hoaDon(null, 1_000_000, HoaDon.HOAN_THANH, "PT01", 1_000_000);
        m.hoaDon(null, 3_000_000, HoaDon.HOAN_THANH, "PT01", 1_000_000, "PT02", 2_000_000); // tiền mặt + chuyển khoản
        HoaDon cu = m.hoaDon(null, 500_000, HoaDon.HOAN_THANH); // đơn cũ: không có lịch sử thanh toán theo phương thức
        cu.setIdPhuongThucThanhToan(3); // COD
        m.hoaDon(null, 700_000, HoaDon.DA_HUY, "PT01", 700_000); // đơn hủy: không tính
        LichSuThanhToan chuaTra = new LichSuThanhToan();
        chuaTra.setHoaDon(m.hoaDons.get(0));
        chuaTra.setIdPhuongThucThanhToan(2);
        chuaTra.setSoTien(BigDecimal.valueOf(999));
        chuaTra.setTrangThai(0);
        m.thanhToans.add(chuaTra); // chưa thanh toán: bỏ qua
        List<Map<String, Object>> pt = ds(service.tongHop(null, null, null), "theoPhuongThuc");
        // bằng tiền thì phương thức nhiều đơn hơn lên trước
        assertEquals(List.of("Tiền mặt", "Chuyển khoản ngân hàng", "COD"), pt.stream().map(x -> x.get("ten")).toList());
        assertEquals(2_000_000L, pt.get(0).get("soTien"));
        assertEquals(2L, pt.get(0).get("soDon"));
        assertEquals(2_000_000L, pt.get(1).get("soTien"));
        assertEquals(1L, pt.get(1).get("soDon"));
        assertEquals(500_000L, pt.get(2).get("soTien"));
    }

    @Test
    void theoGio_vaTheoCa() {
        HoaDon sang = m.hoaDon(null, 1_000_000, HoaDon.HOAN_THANH);
        sang.setNgayTao(HOM_NAY.atTime(7, 0));
        HoaDon chieu = m.hoaDon(null, 2_000_000, HoaDon.HOAN_THANH);
        chieu.setNgayTao(HOM_NAY.atTime(12, 0)); // 12:00 thuộc ca chiều (ca sáng kết thúc 12:00)
        HoaDon dem = m.hoaDon(null, 300_000, HoaDon.HOAN_THANH);
        dem.setNgayTao(HOM_NAY.atTime(23, 30));
        m.caGay.setTrangThai(0); // ca ngừng dùng không tính
        Map<String, Object> kq = service.tongHop(null, null, null);
        List<Map<String, Object>> gio = ds(kq, "theoGio");
        assertEquals(24, gio.size());
        assertEquals(1L, gio.get(7).get("soDon"));
        assertEquals(2_000_000L, gio.get(12).get("doanhThu"));
        List<Map<String, Object>> ca = ds(kq, "theoCa");
        assertEquals(List.of("Ca sáng", "Ca chiều", "Ca tối", "Ngoài giờ ca"), ca.stream().map(x -> x.get("tenCa")).toList());
        assertEquals("07:00 - 12:00", ca.get(0).get("gio"));
        assertEquals(1_000_000L, ca.get(0).get("doanhThu"));
        assertEquals(2_000_000L, ca.get(1).get("doanhThu"));
        assertEquals(0L, ca.get(2).get("soDon"));
        assertEquals(300_000L, ca.get(3).get("doanhThu"));
    }

    @Test
    void chuyenCan_theoLichLamViec() {
        LocalDate homQua = HOM_NAY.minusDays(1);
        LichLamViec baoHomQua = m.lich(m.bao, m.sang, homQua);
        LichLamViec baoSang = m.lich(m.bao, m.sang, HOM_NAY);
        m.lich(m.bao, m.toi, HOM_NAY);                  // ca tối hôm nay: chưa tới (bây giờ 13:00)
        m.lich(m.anh, m.sang, homQua);                  // Anh vắng hôm qua
        m.lich(m.anh, m.sang, HOM_NAY);                 // Anh vắng sáng nay (ca sáng đã kết thúc lúc 12:00)
        LichLamViec nghiLich = m.lich(m.anh, m.chieu, HOM_NAY.plusDays(1));
        nghiLich.setTrangThai(0);                       // lịch đã hủy: bỏ qua
        giaoCa(1, m.bao, baoHomQua, homQua.atTime(7, 0), homQua.atTime(12, 0), 0, 0L);
        giaoCa(2, m.bao, baoSang, HOM_NAY.atTime(7, 40), HOM_NAY.atTime(12, 10), 40, null);
        giaoCa(3, m.anh, null, HOM_NAY.atTime(12, 30), null, 0, null); // ngoài lịch, đang trong ca
        List<Map<String, Object>> cc = ds(service.tongHop(null, null, null), "chuyenCan");
        assertEquals(List.of("Lê Quốc Bảo", "Phạm Minh Anh"), cc.stream().map(x -> x.get("hoTen")).toList());
        Map<String, Object> bao = cc.get(0);
        assertEquals(3L, bao.get("soCaLich"));
        assertEquals(2L, bao.get("soCaDaLam"));
        assertEquals(0L, bao.get("soCaVang"));
        assertEquals(1L, bao.get("soCaChuaToi"));
        assertEquals(1L, bao.get("soLanDiMuon"));
        assertEquals(40L, bao.get("tongPhutMuon"));
        assertEquals(300L + 270L, bao.get("soPhutLam"));
        Map<String, Object> anh = cc.get(1);
        assertEquals(2L, anh.get("soCaLich"));
        assertEquals(2L, anh.get("soCaVang"));
        assertEquals(1L, anh.get("soCaNgoaiLich"));
        assertEquals(0L, anh.get("soPhutLam"));
    }

    @Test
    void daQua_caHomNayTinhTheoGioKetThuc() {
        LichLamViec l = m.lich(m.bao, m.chieu, HOM_NAY);
        assertFalse(ThongKeService.daQua(l, HOM_NAY.atTime(16, 59)));
        assertTrue(ThongKeService.daQua(l, HOM_NAY.atTime(17, 0)));
        assertTrue(ThongKeService.daQua(l, HOM_NAY.plusDays(1).atTime(0, 1)));
        assertFalse(ThongKeService.daQua(l, HOM_NAY.minusDays(1).atTime(23, 0)));
    }

    @Test
    void giaoCa_lechTien() {
        giaoCa(1, m.bao, null, HOM_NAY.atTime(7, 0), HOM_NAY.atTime(12, 0), 0, -50_000L);
        giaoCa(2, m.anh, null, HOM_NAY.atTime(12, 0), HOM_NAY.atTime(17, 0), 0, 200_000L);
        giaoCa(3, m.bao, null, HOM_NAY.minusDays(1).atTime(7, 0), HOM_NAY.minusDays(1).atTime(12, 0), 0, 0L);
        giaoCa(4, m.anh, null, HOM_NAY.atTime(17, 0), null, 0, null); // đang trong ca
        @SuppressWarnings("unchecked")
        Map<String, Object> gc = (Map<String, Object>) service.tongHop(null, null, null).get("giaoCa");
        assertEquals(3L, gc.get("soCaDaGiao"));
        assertEquals(2, gc.get("soCaLech"));
        assertEquals(50_000L, gc.get("tongThieu"));
        assertEquals(200_000L, gc.get("tongThua"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lech = (List<Map<String, Object>>) gc.get("caLech");
        assertEquals(List.of("GC02", "GC01"), lech.stream().map(x -> x.get("maGiaoCa")).toList());
        assertEquals("Thối nhầm", lech.get(1).get("ghiChu"));
    }
}
