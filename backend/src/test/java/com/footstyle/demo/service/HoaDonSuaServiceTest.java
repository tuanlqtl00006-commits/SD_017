package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.KhoGia;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.HoaDonChiTiet;
import com.footstyle.demo.entity.LichSuHoaDon;
import com.footstyle.demo.entity.LichSuThanhToan;
import com.footstyle.demo.entity.SanPham;
import com.footstyle.demo.entity.SanPhamChiTiet;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.HinhAnhSanPhamRepository;
import com.footstyle.demo.repository.HoaDonChiTietRepository;
import com.footstyle.demo.repository.HoaDonRepository;
import com.footstyle.demo.repository.LichSuHoaDonRepository;
import com.footstyle.demo.repository.LichSuThanhToanRepository;
import com.footstyle.demo.repository.SanPhamChiTietRepository;
import com.footstyle.demo.security.XacThuc;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Case khó online: sửa đơn 1 lần khi Chờ xác nhận, tách dòng khi giá đổi, hoàn phí / phụ phí, xác nhận chuyển khoản. */
class HoaDonSuaServiceTest {

    private final CaLamMau m = new CaLamMau();
    private final HoaDon hd = new HoaDon();
    private final List<HoaDonChiTiet> dong = new ArrayList<>();
    private final List<LichSuThanhToan> thanhToan = new ArrayList<>();
    private final List<LichSuHoaDon> lichSu = new ArrayList<>();
    private final List<SanPhamChiTiet> bienThes = new ArrayList<>();
    private final Map<Long, Double> phanTramDot = new HashMap<>();
    private final SanPhamChiTiet cau; // 250.000
    private final SanPhamChiTiet vot; // 1.200.000
    private final HoaDonSuaService service;
    private int idCt = 1;

    HoaDonSuaServiceTest() {
        cau = bienThe(1, "SP07-TRANG", 250_000, 10);
        vot = bienThe(2, "SP01-DEN", 1_200_000, 3);
        hd.setId(1);
        hd.setMaHoaDon("HD01");
        hd.setLoaiDon(HoaDon.GIAO_HANG);
        hd.setTrangThai(HoaDon.CHO_XAC_NHAN);
        hd.setHoTenNguoiNhan("Nguyễn Thị Lan");
        hd.setSdtNguoiNhan("0901000001");
        hd.setDiaChiGiaoHang("Số 15 Trần Phú, Hà Đông, Hà Nội");
        hd.setIdPhuongThucThanhToan(HoaDonSuaService.CHUYEN_KHOAN_ONLINE);
        hd.setNgayTao(LocalDateTime.of(2026, 10, 20, 9, 0));
        themDong(cau, 2, 250_000); // 500.000 + ship 30.000 = 530.000
        hd.setTongTien(BigDecimal.valueOf(500_000));
        hd.setSoTienGiam(BigDecimal.ZERO);
        hd.setPhiVanChuyen(BigDecimal.valueOf(30_000));
        hd.setThanhTien(BigDecimal.valueOf(530_000));

        HoaDonRepository hdRepo = KhoGia.tao(HoaDonRepository.class, (t, a) -> switch (t) {
            case "findById" -> Integer.valueOf(1).equals(a[0]) ? Optional.of(hd) : Optional.empty();
            case "save" -> a[0];
            case "findTenPhuongThucThanhToan" -> "Chuyển khoản ngân hàng";
            default -> KhoGia.KHONG_CO;
        });
        HoaDonChiTietRepository ctRepo = KhoGia.tao(HoaDonChiTietRepository.class, (t, a) -> switch (t) {
            case "findByHoaDon_IdOrderByIdAsc" -> new ArrayList<>(dong);
            case "save" -> {
                HoaDonChiTiet ct = (HoaDonChiTiet) a[0];
                if (ct.getId() == null) {
                    ct.setId(idCt++);
                    dong.add(ct);
                }
                yield ct;
            }
            case "delete" -> dong.remove(a[0]);
            default -> KhoGia.KHONG_CO;
        });
        LichSuHoaDonRepository lsRepo = KhoGia.tao(LichSuHoaDonRepository.class, (t, a) -> switch (t) {
            case "findByHoaDon_IdOrderByThoiGianAscIdAsc" -> new ArrayList<>(lichSu);
            case "save" -> {
                lichSu.add((LichSuHoaDon) a[0]);
                yield a[0];
            }
            default -> KhoGia.KHONG_CO;
        });
        LichSuThanhToanRepository ttRepo = KhoGia.tao(LichSuThanhToanRepository.class, (t, a) -> switch (t) {
            case "findByHoaDon_IdOrderByThoiGianAscIdAsc" -> new ArrayList<>(thanhToan);
            case "save" -> {
                thanhToan.add((LichSuThanhToan) a[0]);
                yield a[0];
            }
            default -> KhoGia.KHONG_CO;
        });
        SanPhamChiTietRepository spctRepo = KhoGia.tao(SanPhamChiTietRepository.class, (t, a) ->
                "findById".equals(t) ? bienThes.stream().filter(c -> c.getId().equals(a[0])).findFirst() : KhoGia.KHONG_CO);
        HoaDonService hoaDonService = new HoaDonService(hdRepo, ctRepo, lsRepo, ttRepo,
                KhoGia.tao(HinhAnhSanPhamRepository.class, (t, a) -> "findByTrangThaiOrderByLaAnhChinhDescIdAsc".equals(t) ? List.of() : KhoGia.KHONG_CO), spctRepo);
        GiamGiaApDungService giam = new GiamGiaApDungService(null) {
            @Override
            public Map<Long, Double> phanTramTheoBienThe(LocalDateTime thoiDiem) {
                return phanTramDot;
            }
        };
        service = new HoaDonSuaService(hdRepo, ctRepo, spctRepo, lsRepo, ttRepo, giam, hoaDonService);
        CaLamMau.dangNhap(m.bao);
    }

    @AfterEach
    void don() {
        XacThuc.xoa();
    }

    private SanPhamChiTiet bienThe(int id, String ma, long gia, int ton) {
        SanPham sp = new SanPham();
        sp.setId(id);
        sp.setTenSanPham("SP " + ma);
        sp.setTrangThai(1);
        SanPhamChiTiet c = new SanPhamChiTiet();
        c.setId(id);
        c.setMaSpct(ma);
        c.setSanPham(sp);
        c.setGiaBan(gia);
        c.setSoLuongTon(ton);
        c.setTrangThai(1);
        bienThes.add(c);
        return c;
    }

    private void themDong(SanPhamChiTiet c, int sl, long gia) {
        HoaDonChiTiet ct = new HoaDonChiTiet();
        ct.setId(idCt++);
        ct.setHoaDon(hd);
        ct.setSanPhamChiTiet(c);
        ct.setSoLuong(sl);
        ct.setDonGia(BigDecimal.valueOf(gia));
        ct.setThanhTien(BigDecimal.valueOf(gia * sl));
        dong.add(ct);
    }

    private void daChuyenKhoan(long tien) {
        LichSuThanhToan t = new LichSuThanhToan();
        t.setSoTien(BigDecimal.valueOf(tien));
        t.setTrangThai(LichSuThanhToan.DA_THANH_TOAN);
        thanhToan.add(t);
    }

    private HoaDonSuaService.SuaDonRequest sua(String diaChi, HoaDonSuaService.DongSua... items) {
        return new HoaDonSuaService.SuaDonRequest("Nguyễn Thị Lan", "0901000001", diaChi, List.of(items), "Khách gọi điện đổi đơn");
    }

    @Test
    void themCungSanPhamKhiGiaDaDoi_tachDongMoi_vaChiSuaDuocMotLan() {
        phanTramDot.put(1L, 20.0); // cầu đang giảm 20%: 200.000
        service.suaDon(1, sua("Số 15 Trần Phú, Hà Đông, Hà Nội",
                new HoaDonSuaService.DongSua(1, 1, 2), new HoaDonSuaService.DongSua(null, 1, 1)));
        assertEquals(2, dong.size(), "dòng cũ giữ giá lúc đặt 250.000, dòng mới theo giá 200.000");
        assertEquals(0, BigDecimal.valueOf(200_000).compareTo(dong.get(1).getDonGia()));
        assertEquals(0, BigDecimal.valueOf(730_000).compareTo(hd.getThanhTien()), "700.000 + ship 30.000");
        assertTrue(lichSu.get(0).getMoTa().contains("tách dòng mới"));
        ApiException e = assertThrows(ApiException.class, () -> service.suaDon(1, sua("Số 99 Lê Lợi, Hà Đông, Hà Nội", new HoaDonSuaService.DongSua(1, 1, 1))));
        assertEquals("DA_SUA_DON", e.getCode());
    }

    @Test
    void khachDaTraTruoc_bonSanPham_ghiHoanPhi() {
        daChuyenKhoan(530_000);
        Map<String, Object> kq = service.suaDon(1, sua("Số 99 Lê Lợi, Hà Đông, Hà Nội", new HoaDonSuaService.DongSua(1, 1, 1)));
        assertEquals(0, BigDecimal.valueOf(280_000).compareTo(hd.getThanhTien()), "250.000 + ship 30.000");
        assertEquals(2, thanhToan.size());
        assertEquals(0, BigDecimal.valueOf(-250_000).compareTo(thanhToan.get(1).getSoTien()), "hoàn lại 530.000 - 280.000");
        assertEquals("Số 99 Lê Lợi, Hà Đông, Hà Nội", hd.getDiaChiGiaoHang());
        @SuppressWarnings("unchecked")
        Map<String, Object> ketQua = (Map<String, Object>) kq.get("ketQuaSua");
        assertEquals(250_000L, ketQua.get("hoanPhi"));
    }

    @Test
    void khachDaTraTruoc_themSanPham_phuPhiThuKhiGiao_mienShip() {
        daChuyenKhoan(530_000);
        service.suaDon(1, sua("Số 15 Trần Phú, Hà Đông, Hà Nội",
                new HoaDonSuaService.DongSua(1, 1, 2), new HoaDonSuaService.DongSua(null, 2, 1)));
        assertEquals(0, BigDecimal.ZERO.compareTo(hd.getPhiVanChuyen()), "đơn từ 1.000.000 miễn phí giao hàng");
        assertEquals(0, BigDecimal.valueOf(1_700_000).compareTo(hd.getThanhTien()));
        assertEquals(1, thanhToan.size(), "phụ phí thu khi giao hàng, chưa ghi thanh toán");
        assertTrue(lichSu.get(0).getMoTa().contains("Phụ phí 1.170.000 ₫"));
    }

    @Test
    void kiemTraDuLieu_vaChiSuaKhiChoXacNhan() {
        ApiException e = assertThrows(ApiException.class, () -> service.suaDon(1,
                new HoaDonSuaService.SuaDonRequest(" ", "123", "ngắn", List.of(), " ")));
        assertEquals(List.of("hoTenNguoiNhan", "sdtNguoiNhan", "diaChiGiaoHang", "lyDo", "items"), List.copyOf(e.getLoiTheoTruong().keySet()));
        assertThrows(ApiException.class, () -> service.suaDon(1, sua("Số 15 Trần Phú, Hà Đông, Hà Nội", new HoaDonSuaService.DongSua(null, 2, 9))),
                "kho chỉ còn 3");
        hd.setTrangThai(HoaDon.DA_XAC_NHAN);
        assertThrows(ApiException.class, () -> service.suaDon(1, sua("Số 15 Trần Phú, Hà Đông, Hà Nội", new HoaDonSuaService.DongSua(1, 1, 1))));
    }

    @Test
    void xacNhanChuyenKhoan_ghiDaThanhToanPhanConLai() {
        service.xacNhanThanhToan(1, new HoaDonSuaService.XacNhanThanhToanRequest("FT26102001"));
        assertEquals(1, thanhToan.size());
        assertEquals(0, BigDecimal.valueOf(530_000).compareTo(thanhToan.get(0).getSoTien()));
        assertEquals("FT26102001", thanhToan.get(0).getMaGiaoDich());
        assertThrows(ApiException.class, () -> service.xacNhanThanhToan(1, null), "đã trả đủ");
    }
}
