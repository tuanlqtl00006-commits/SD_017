package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.KhoGia;
import com.footstyle.demo.entity.ChuVi;
import com.footstyle.demo.entity.GiaoCa;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.MauSac;
import com.footstyle.demo.entity.SanPham;
import com.footstyle.demo.entity.SanPhamChiTiet;
import com.footstyle.demo.entity.TrongLuong;
import com.footstyle.demo.repository.HoaDonRepository;
import com.footstyle.demo.repository.SanPhamChiTietRepository;
import com.footstyle.demo.security.NguoiDung;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class TroLyNoiBoServiceTest {

    private static final LocalDate HOM_NAY = CaLamMau.HOM_NAY; // thứ 6, 16/10/2026
    private static final NguoiDung QUAN_LY = new NguoiDung("NV", 1, "NV01", "Nguyễn Hoàng Long", NguoiDung.QUAN_LY, null);
    private static final NguoiDung BAO = new NguoiDung("NV", 3, "NV03", "Lê Quốc Bảo", NguoiDung.NHAN_VIEN, null);

    private final CaLamMau m = new CaLamMau();
    private final List<SanPhamChiTiet> bienThes = new ArrayList<>();
    private final TroLyNoiBoService troLy;
    private String cauHoiChuyenChoKhach;

    TroLyNoiBoServiceTest() {
        SanPham astrox = new SanPham();
        astrox.setTenSanPham("Yonex Astrox 99 Pro");
        bienThe(1, astrox, "Đỏ", "3U", 3);
        bienThe(2, astrox, "Đen", "4U", 20);
        SanPham cau = new SanPham();
        cau.setTenSanPham("Ống cầu Thành Công 77");
        bienThe(3, cau, "Trắng", null, 80);

        hoaDon(1, "HD51", HoaDon.HOAN_THANH, 1_200_000, HoaDon.TAI_QUAY, HOM_NAY.atTime(8, 0));
        hoaDon(2, "HD52", HoaDon.HOAN_THANH, 800_000, HoaDon.GIAO_HANG, HOM_NAY.atTime(9, 30));
        hoaDon(3, "HD53", HoaDon.HOAN_THANH, 5_000_000, HoaDon.TAI_QUAY, HOM_NAY.minusDays(1).atTime(15, 0));
        hoaDon(4, "HD54", HoaDon.CHO_XAC_NHAN, 450_000, HoaDon.GIAO_HANG, HOM_NAY.atTime(9, 45));

        HoaDonRepository hoaDonRepo = KhoGia.tao(HoaDonRepository.class, (t, a) -> switch (t) {
            case "findFirstByMaHoaDonIgnoreCase" -> m.hoaDons.stream().filter(h -> h.getMaHoaDon().equalsIgnoreCase((String) a[0])).findFirst();
            case "findByTrangThai" -> new PageImpl<>(m.hoaDons.stream().filter(h -> h.getTrangThai().equals(a[0])).toList(), (Pageable) a[1],
                    m.hoaDons.stream().filter(h -> h.getTrangThai().equals(a[0])).count());
            case "findByTrangThaiAndNgayTaoBetween" -> m.hoaDons.stream().filter(h -> h.getTrangThai().equals(a[0])
                    && !h.getNgayTao().isBefore((LocalDateTime) a[1]) && !h.getNgayTao().isAfter((LocalDateTime) a[2])).toList();
            case "findByIdGiaoCaOrderByNgayTaoAsc" -> m.hoaDons.stream().filter(h -> Objects.equals(h.getIdGiaoCa(), a[0])).toList();
            default -> KhoGia.KHONG_CO;
        });
        SanPhamChiTietRepository spctRepo = KhoGia.tao(SanPhamChiTietRepository.class, (t, a) -> switch (t) {
            case "findAllByOrderByIdDesc" -> bienThes.stream().sorted(Comparator.comparing(SanPhamChiTiet::getId).reversed()).toList();
            case "findByTrangThaiAndSoLuongTonLessThanEqualOrderBySoLuongTonAscIdAsc" -> bienThes.stream()
                    .filter(c -> c.getTrangThai().equals(a[0]) && c.getSoLuongTon() <= (Integer) a[1]).toList();
            default -> KhoGia.KHONG_CO;
        });
        TroLyAoService khach = new TroLyAoService(null, null, null, null, null, "FootStyle", "", "", "") {
            @Override
            public TraLoi traLoi(String cauHoi, NguCanh nc) {
                cauHoiChuyenChoKhach = cauHoi;
                return cauHoi.contains("vợt")
                        ? new TraLoi("Gợi ý vợt: Astrox 99 Pro", false, List.of())
                        : new TraLoi("Xin lỗi, mình chưa hiểu câu hỏi.", false, List.of());
            }
        };
        troLy = new TroLyNoiBoService(hoaDonRepo, spctRepo, m.lichRepo(), m.giaoCaRepo(), khach);
        troLy.datDongHo(CaLamMau.luc(HOM_NAY, "10:00"));
    }

    private void bienThe(int id, SanPham sp, String mau, String tl, int ton) {
        SanPhamChiTiet c = new SanPhamChiTiet();
        c.setId(id);
        c.setSanPham(sp);
        c.setMaSpct("SP01-" + id);
        MauSac ms = new MauSac();
        ms.setTen(mau);
        c.setMauSac(ms);
        TrongLuong t = new TrongLuong();
        t.setMa(tl == null ? "TL000" : "TL" + id);
        t.setTen(tl == null ? "Không áp dụng" : tl);
        c.setTrongLuong(t);
        ChuVi cv = new ChuVi();
        cv.setMa("CV000");
        cv.setTen("Không áp dụng");
        c.setChuVi(cv);
        c.setSoLuongTon(ton);
        c.setTrangThai(1);
        bienThes.add(c);
    }

    private HoaDon hoaDon(int id, String ma, int tt, long tien, int loai, LocalDateTime luc) {
        HoaDon h = new HoaDon();
        h.setId(id);
        h.setMaHoaDon(ma);
        h.setTrangThai(tt);
        h.setThanhTien(BigDecimal.valueOf(tien));
        h.setLoaiDon(loai);
        h.setNgayTao(luc);
        m.hoaDons.add(h);
        return h;
    }

    private TroLyNoiBoService.TraLoi hoi(String cau, NguoiDung nd) {
        return troLy.traLoi(cau, nd);
    }

    @Test
    void doanhThu_chiQuanLyXemDuoc() {
        TroLyNoiBoService.TraLoi ql = hoi("Doanh thu hôm nay bao nhiêu?", QUAN_LY);
        assertTrue(ql.noiDung().startsWith("Doanh thu hôm nay (16/10/2026): 2.000.000 ₫"), ql.noiDung());
        assertTrue(ql.noiDung().contains("2 đơn hoàn thành (1 tại quầy, 1 giao hàng)"));
        assertEquals("/thong-ke", ql.lienKet().get(0).duongDan());
        assertTrue(hoi("doanh thu hom qua", QUAN_LY).noiDung().contains("5.000.000 ₫"));
        assertTrue(hoi("doanh thu tuần này", QUAN_LY).noiDung().contains("12/10/2026 - 16/10/2026): 7.000.000 ₫"));

        TroLyNoiBoService.TraLoi nv = hoi("doanh thu hôm nay", BAO);
        assertTrue(nv.noiDung().startsWith("Doanh thu toàn cửa hàng chỉ Quản lý xem được."));
        assertFalse(nv.noiDung().contains("2.000.000"));
        assertTrue(nv.lienKet().isEmpty());
    }

    @Test
    void donChoXacNhan_vaTraMaDon() {
        TroLyNoiBoService.TraLoi cho = hoi("có đơn nào chờ xác nhận không", BAO);
        assertTrue(cho.noiDung().startsWith("Có 1 đơn đang chờ xác nhận:"));
        assertTrue(cho.noiDung().contains("HD54 · 450.000 ₫"));

        TroLyNoiBoService.TraLoi don = hoi("xem giúp đơn hd 51", BAO);
        assertTrue(don.noiDung().startsWith("Hóa đơn HD51 (tại quầy):"));
        assertTrue(don.noiDung().contains("Khách vãng lai"), "nhân viên tra đơn không cần số điện thoại");
        assertEquals("/hoa-don/1", don.lienKet().get(0).duongDan());
        assertEquals("Không có hóa đơn HD999.", hoi("HD999", BAO).noiDung());
    }

    @Test
    void tonKho_vaSapHetHang() {
        String ton = hoi("tồn kho astrox đỏ", BAO).noiDung();
        assertTrue(ton.startsWith("Tồn kho (1 phân loại, tổng 3):"), ton);
        assertTrue(ton.contains("Yonex Astrox 99 Pro · Đỏ · 3U [SP01-1]: 3"));
        assertTrue(hoi("astrox còn bao nhiêu", BAO).noiDung().contains("2 phân loại, tổng 23"));
        String het = hoi("sản phẩm sắp hết hàng", BAO).noiDung();
        assertTrue(het.startsWith("1 phân loại còn ≤ 5:"));
        assertEquals("Ống cầu Thành Công 77 · Trắng", TroLyNoiBoService.tenBienThe(bienThes.get(2)), "bỏ trọng lượng / chu vi \"Không áp dụng\"");
    }

    @Test
    void lichHomNay_lichCuaToi_caCuaToi() {
        m.lich(m.bao, m.sang, HOM_NAY);
        m.lich(m.anh, m.sang, HOM_NAY);
        m.lich(m.bao, m.chieu, HOM_NAY.plusDays(2));
        String homNay = hoi("Ai làm ca hôm nay?", BAO).noiDung();
        assertTrue(homNay.contains("Ca sáng (07:00 - 12:00): Lê Quốc Bảo, Phạm Minh Anh"), homNay);

        String cuaToi = hoi("lịch của tôi", BAO).noiDung();
        assertTrue(cuaToi.contains("Thứ 6 16/10/2026: Ca sáng"));
        assertTrue(cuaToi.contains("Chủ nhật 18/10/2026: Ca chiều"));

        assertTrue(hoi("ca của tôi", BAO).noiDung().startsWith("Bạn chưa nhận ca."));
        GiaoCa ca = new GiaoCa();
        ca.setId(1);
        ca.setMaGiaoCa("GC07");
        ca.setNhanVien(m.bao);
        ca.setTrangThai(GiaoCa.DANG_TRONG_CA);
        ca.setThoiGianNhanCa(HOM_NAY.atTime(7, 0));
        ca.setTienDauCa(BigDecimal.valueOf(1_000_000));
        m.giaoCas.add(ca);
        m.hoaDons.get(0).setIdGiaoCa(1);
        String dangCa = hoi("ca của tôi", BAO).noiDung();
        assertTrue(dangCa.contains("ca GC07 từ 07:00 16/10/2026: 1 đơn hoàn thành, doanh thu 1.200.000 ₫"), dangCa);
        assertTrue(hoi("doanh thu", BAO).noiDung().contains("ca GC07"), "nhân viên hỏi doanh thu thì xem được doanh thu ca của mình");
    }

    @Test
    void huongDanThaoTac_theoVaiTro() {
        TroLyNoiBoService.TraLoi gc = hoi("cách giao ca thế nào", BAO);
        assertTrue(gc.noiDung().startsWith("Nhận / giao ca:"));
        assertEquals("/giao-ca", gc.lienKet().get(0).duongDan());
        assertTrue(hoi("làm sao nhập excel lịch", QUAN_LY).noiDung().startsWith("Xếp lịch:"));
        assertEquals("Thao tác này chỉ Quản lý làm được. Bạn nhờ Quản lý hỗ trợ nhé.", hoi("hướng dẫn xếp lịch", BAO).noiDung());
    }

    @Test
    void sanPhamKhuyenMai_dungLaiTroLyKhach_conLaiBaoChuaHieu() {
        assertEquals("Gợi ý vợt: Astrox 99 Pro", hoi("vợt dưới 3 triệu", BAO).noiDung());
        assertEquals("vợt dưới 3 triệu", cauHoiChuyenChoKhach);
        TroLyNoiBoService.TraLoi khongHieu = hoi("thời tiết mai thế nào", BAO);
        assertTrue(khongHieu.noiDung().startsWith("Mình chưa hiểu câu hỏi."));
        assertEquals(TroLyNoiBoService.GOI_Y_NHAN_VIEN, khongHieu.goiY());
        assertTrue(troLy.loiChao(QUAN_LY).noiDung().contains("Doanh thu"));
        assertFalse(troLy.loiChao(BAO).noiDung().contains("Doanh thu"));
    }
}
