package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.KhoGia;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.HoaDonChiTiet;
import com.footstyle.demo.entity.SanPham;
import com.footstyle.demo.entity.SanPhamChiTiet;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.HoaDonChiTietRepository;
import com.footstyle.demo.repository.HoaDonRepository;
import com.footstyle.demo.repository.NhanVienRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ThongKeSoSanhServiceTest {

    private final List<HoaDon> hoaDons = new ArrayList<>();
    private final List<HoaDonChiTiet> chiTiets = new ArrayList<>();
    private final Map<String, Object> thuDaGui = new LinkedHashMap<>();
    private final ThongKeSoSanhService service;
    private final SanPhamChiTiet vot;

    ThongKeSoSanhServiceTest() {
        SanPham sp = new SanPham();
        sp.setId(1);
        sp.setMaSanPham("SP01");
        sp.setTenSanPham("Vợt Yonex Astrox 99 Pro");
        vot = new SanPhamChiTiet();
        vot.setId(1);
        vot.setSanPham(sp);

        MailService mail = new MailService(null, "", "", "", "FootStyle", "") {
            @Override
            public boolean guiBaoCao(String den, String tieuDe, String banChu, String html, Map<String, byte[]> anh) {
                thuDaGui.put("den", den);
                thuDaGui.put("tieuDe", tieuDe);
                thuDaGui.put("html", html);
                thuDaGui.put("anh", anh);
                return false;
            }
        };
        service = new ThongKeSoSanhService(
                KhoGia.tao(HoaDonRepository.class, (t, a) -> "findByNgayTaoBetween".equals(t)
                        ? hoaDons.stream().filter(h -> !h.getNgayTao().isBefore((LocalDateTime) a[0]) && !h.getNgayTao().isAfter((LocalDateTime) a[1])).toList()
                        : KhoGia.KHONG_CO),
                KhoGia.tao(HoaDonChiTietRepository.class, (t, a) -> "findByHoaDon_TrangThaiAndHoaDon_NgayTaoBetween".equals(t)
                        ? chiTiets.stream().filter(c -> c.getHoaDon().getTrangThai().equals(a[0])).toList() : KhoGia.KHONG_CO),
                KhoGia.tao(NhanVienRepository.class, (t, a) -> "findByTrangThaiOrderByHoTenAsc".equals(t) ? List.of() : KhoGia.KHONG_CO),
                mail);
        service.datDongHo(Clock.fixed(LocalDateTime.of(2026, 10, 24, 10, 0).atZone(CaLamMau.VN).toInstant(), CaLamMau.VN));

        // Tháng 10/2026: 2 đơn hoàn thành (500k giảm 50k + ship 30k -> doanh thu 450k; 1.000.000), 1 đơn đang giao 300k, 1 đơn hủy
        don(LocalDateTime.of(2026, 10, 3, 9, 0), HoaDon.HOAN_THANH, HoaDon.GIAO_HANG, 480_000, 30_000, 2);
        don(LocalDateTime.of(2026, 10, 5, 15, 0), HoaDon.HOAN_THANH, HoaDon.TAI_QUAY, 1_000_000, 0, 1);
        don(LocalDateTime.of(2026, 10, 10, 11, 0), HoaDon.DANG_GIAO, HoaDon.GIAO_HANG, 330_000, 30_000, 0);
        don(LocalDateTime.of(2026, 10, 11, 11, 0), HoaDon.DA_HUY, HoaDon.GIAO_HANG, 999_000, 0, 0);
        don(LocalDateTime.of(2026, 10, 12, 11, 0), HoaDon.CHO_XAC_NHAN, HoaDon.TAI_QUAY, 777_000, 0, 0); // hóa đơn chờ tại quầy: bỏ
        // Tháng 9/2026: 1 đơn 1.200.000; tháng 10/2025 (cùng kỳ): 1 đơn 725.000
        don(LocalDateTime.of(2026, 9, 20, 9, 0), HoaDon.HOAN_THANH, HoaDon.TAI_QUAY, 1_200_000, 0, 1);
        don(LocalDateTime.of(2025, 10, 8, 9, 0), HoaDon.HOAN_THANH, HoaDon.TAI_QUAY, 725_000, 0, 1);
    }

    private void don(LocalDateTime luc, int trangThai, int loai, long thanhTien, long ship, int soLuongVot) {
        HoaDon h = new HoaDon();
        h.setId(hoaDons.size() + 1);
        h.setMaHoaDon("HD" + h.getId());
        h.setNgayTao(luc);
        h.setTrangThai(trangThai);
        h.setLoaiDon(loai);
        h.setThanhTien(BigDecimal.valueOf(thanhTien));
        h.setPhiVanChuyen(BigDecimal.valueOf(ship));
        h.setTongTien(BigDecimal.valueOf(thanhTien - ship));
        hoaDons.add(h);
        if (soLuongVot > 0) {
            HoaDonChiTiet ct = new HoaDonChiTiet();
            ct.setHoaDon(h);
            ct.setSanPhamChiTiet(vot);
            ct.setSoLuong(soLuongVot);
            ct.setThanhTien(BigDecimal.valueOf(thanhTien - ship));
            chiTiets.add(ct);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void soSanhThang_thucTeChiDonHoanThanh_duKienGomDonDangXuLy_khongTinhShipVaHoaDonCho() {
        Map<String, Object> kq = service.soSanh("THANG", "2026-10");
        Map<String, Object> a = (Map<String, Object>) kq.get("hienTai");
        assertEquals(1_450_000L, a.get("doanhThuThucTe"), "450k (bỏ ship 30k) + 1.000.000");
        assertEquals(1_750_000L, a.get("doanhThuDuKien"), "cộng thêm đơn đang giao 300k (bỏ ship), không tính đơn hủy");
        assertEquals(4, a.get("soDon"), "bỏ hóa đơn chờ tại quầy");
        assertEquals(2, a.get("soHoanThanh"));
        assertEquals(1, a.get("soHuy"));
        assertEquals(3L, a.get("soSanPhamBan"));
        assertEquals(725_000L, a.get("giaTriTrungBinh"));

        Map<String, Object> tangKt = (Map<String, Object>) kq.get("tangSoVoiKyTruoc");
        assertEquals(20.8, tangKt.get("doanhThuThucTe"), "1.450.000 so với 1.200.000 tháng 9");
        Map<String, Object> tangCk = (Map<String, Object>) kq.get("tangSoVoiCungKy");
        assertEquals(100.0, tangCk.get("doanhThuThucTe"), "gấp đôi tháng 10/2025");

        List<Map<String, Object>> xu = (List<Map<String, Object>>) kq.get("xuHuong");
        assertEquals(12, xu.size());
        assertEquals(1_450_000L, xu.get(9).get("doanhThu"));
        assertEquals(725_000L, xu.get(9).get("doanhThuNamTruoc"));
        assertEquals(31, ((List<?>) kq.get("chiTietKy")).size(), "tháng 10 có 31 ngày");
        List<Map<String, Object>> sp = (List<Map<String, Object>>) kq.get("sanPham");
        assertEquals(3L, sp.get(0).get("soLuong"));
        assertEquals(1L, sp.get(0).get("soLuongKyTruoc"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void soSanhQuyVaNam() {
        Map<String, Object> q = service.soSanh("QUY", "2026-Q4");
        assertEquals("Quý 4/2026", ((Map<String, Object>) q.get("ky")).get("nhan"));
        assertEquals("Quý 3/2026", ((Map<String, Object>) ((Map<String, Object>) q.get("kyTruoc")).get("ky")).get("nhan"));
        assertEquals(3, ((List<?>) q.get("chiTietKy")).size());
        Map<String, Object> n = service.soSanh("NAM", null);
        assertEquals("Năm 2026", ((Map<String, Object>) n.get("ky")).get("nhan"));
        assertEquals(5, ((List<?>) n.get("xuHuong")).size());
        assertNull(n.get("cungKyNamTruoc"), "so sánh năm thì kỳ trước chính là năm trước");
        assertEquals(2_650_000L, ((Map<String, Object>) n.get("hienTai")).get("doanhThuThucTe"));
    }

    @Test
    void kyKhongHopLe_baoLoi() {
        assertThrows(ApiException.class, () -> service.soSanh("TUAN", null));
        assertThrows(ApiException.class, () -> service.soSanh("THANG", "2026-13"));
        assertThrows(ApiException.class, () -> service.soSanh("QUY", "2026-Q5"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void guiEmail_kemBangVaHaiBieuDoPng() {
        Map<String, Object> kq = service.guiEmail("THANG", "2026-10", "quanly@gmail.com");
        assertEquals(true, kq.get("giaLap"));
        assertTrue(String.valueOf(thuDaGui.get("tieuDe")).contains("Tháng 10/2026"));
        String html = String.valueOf(thuDaGui.get("html"));
        assertTrue(html.contains("Doanh thu thực tế") && html.contains("cid:bieu-do-xu-huong"));
        Map<String, byte[]> anh = (Map<String, byte[]>) thuDaGui.get("anh");
        assertEquals(2, anh.size());
        byte[] png = anh.get("bieu-do-xu-huong");
        assertTrue(png.length > 1000 && png[1] == 'P' && png[2] == 'N' && png[3] == 'G');
        assertThrows(ApiException.class, () -> service.guiEmail("THANG", null, "khong-phai-email"));
    }
}
