package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.KhoGia;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.HoaDonChiTiet;
import com.footstyle.demo.entity.KhachHang;
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
    /** id phiếu đã được trả lại lượt (giamLuotDung). */
    private final List<Integer> phieuTraLuot = new ArrayList<>();
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
            case "findFirstByMaHoaDonIgnoreCase" -> "HD01".equalsIgnoreCase(String.valueOf(a[0])) ? Optional.of(hd) : Optional.empty();
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
        SanPhamChiTietRepository spctRepo = KhoGia.tao(SanPhamChiTietRepository.class, (t, a) -> {
            if ("findById".equals(t)) return bienThes.stream().filter(c -> c.getId().equals(a[0])).findFirst();
            if ("truTonKho".equals(t)) { // giống câu UPDATE có điều kiện: chỉ trừ khi còn đủ
                SanPhamChiTiet b = bienThes.stream().filter(c -> c.getId().equals(a[0])).findFirst().orElseThrow();
                int sl = (Integer) a[1];
                if (b.getSoLuongTon() == null || b.getSoLuongTon() < sl) return 0;
                b.setSoLuongTon(b.getSoLuongTon() - sl);
                return 1;
            }
            return KhoGia.KHONG_CO;
        });
        HoaDonService hoaDonService = new HoaDonService(hdRepo, ctRepo, lsRepo, ttRepo,
                KhoGia.tao(HinhAnhSanPhamRepository.class, (t, a) -> "findByTrangThaiOrderByLaAnhChinhDescIdAsc".equals(t) ? List.of() : KhoGia.KHONG_CO), spctRepo,
                KhoGia.tao(PhieuGiamGiaRepository.class, (t, a) -> {
                    if ("giamLuotDung".equals(t)) {
                        phieuTraLuot.add((Integer) a[0]);
                        return 1;
                    }
                    return KhoGia.KHONG_CO;
                }),
                KhoGia.tao(PhieuGiamGiaKhachHangRepository.class, (t, a) -> "findByIdKhachHang".equals(t) ? List.of() : KhoGia.KHONG_CO));
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
        service.suaDon(1, sua("Số 16 Trần Phú, Hà Đông, Hà Nội",
                new HoaDonSuaService.DongSua(1, 1, 2), new HoaDonSuaService.DongSua(null, 1, 1)));
        assertEquals(2, dong.size(), "dòng cũ giữ giá lúc đặt 250.000, dòng mới theo giá 200.000");
        assertEquals(0, BigDecimal.valueOf(200_000).compareTo(dong.get(1).getDonGia()));
        assertEquals(0, BigDecimal.valueOf(730_000).compareTo(hd.getThanhTien()), "700.000 + phí 30.000 của đơn (phí không theo địa chỉ)");
        assertTrue(lichSu.get(0).getMoTa().contains("tách dòng mới"));
        ApiException e = assertThrows(ApiException.class, () -> service.suaDon(1, sua("Số 99 Lê Lợi, Hà Đông, Hà Nội", new HoaDonSuaService.DongSua(1, 1, 1))));
        assertEquals("DA_SUA_DON", e.getCode());
    }

    @Test
    void khachDaTraTruoc_bonSanPham_ghiHoanPhi() {
        daChuyenKhoan(530_000);
        Map<String, Object> kq = service.suaDon(1, sua("Số 99 Lê Lợi, Hà Đông, Hà Nội", new HoaDonSuaService.DongSua(1, 1, 1)));
        assertEquals(0, BigDecimal.valueOf(280_000).compareTo(hd.getThanhTien()), "250.000 + phí 30.000 của đơn");
        assertEquals(2, thanhToan.size());
        assertEquals(0, BigDecimal.valueOf(-250_000).compareTo(thanhToan.get(1).getSoTien()), "hoàn lại 530.000 - 280.000");
        assertEquals("Số 99 Lê Lợi, Hà Đông, Hà Nội", hd.getDiaChiGiaoHang());
        @SuppressWarnings("unchecked")
        Map<String, Object> ketQua = (Map<String, Object>) kq.get("ketQuaSua");
        assertEquals(250_000L, ketQua.get("hoanPhi"));
    }

    @Test
    void doiDiaChiSangTinhKhac_giuPhiShip_khongTinhTheoTinh() {
        // Góp ý 16/11: bỏ bảng phí theo tỉnh, đổi địa chỉ không làm đổi phí giao
        service.suaDon(1, sua("Số 8 Lê Duẩn, Phường Bến Nghé, Thành phố Hồ Chí Minh", new HoaDonSuaService.DongSua(1, 1, 2)));
        assertEquals(0, BigDecimal.valueOf(30_000).compareTo(hd.getPhiVanChuyen()), "giữ phí 30.000 của đơn");
        assertEquals(0, BigDecimal.valueOf(530_000).compareTo(hd.getThanhTien()), "500.000 + ship 30.000");
    }

    @Test
    void giuNguyenDiaChi_giuPhiShipCuaDon() {
        service.suaDon(1, sua("Số 15 Trần Phú, Hà Đông, Hà Nội", new HoaDonSuaService.DongSua(1, 1, 1)));
        assertEquals(0, BigDecimal.valueOf(30_000).compareTo(hd.getPhiVanChuyen()), "chỉ sửa số lượng thì giữ phí nhân viên đã nhập lúc bán");
    }

    @Test
    void diaChiKhongNhanRaTinh_giuPhiShipCu() {
        service.suaDon(1, sua("Số 15 Trần Phú, khu phố 3", new HoaDonSuaService.DongSua(1, 1, 2)));
        assertEquals(0, BigDecimal.valueOf(30_000).compareTo(hd.getPhiVanChuyen()), "không biết tỉnh nên giữ phí cũ");
    }

    @Test
    void khachDaTraTruoc_themSanPham_phuPhiThuKhiGiao_mienShip() {
        daChuyenKhoan(530_000);
        service.suaDon(1, sua("Số 16 Trần Phú, Hà Đông, Hà Nội",
                new HoaDonSuaService.DongSua(1, 1, 2), new HoaDonSuaService.DongSua(null, 2, 1)));
        assertEquals(0, BigDecimal.valueOf(30_000).compareTo(hd.getPhiVanChuyen()), "giữ phí 30.000 của đơn");
        assertEquals(0, BigDecimal.valueOf(1_730_000).compareTo(hd.getThanhTien()));
        assertEquals(1, thanhToan.size(), "phụ phí thu khi giao hàng, chưa ghi thanh toán");
        assertTrue(lichSu.get(0).getMoTa().contains("Phụ phí 1.200.000 ₫"), lichSu.get(0).getMoTa());
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

    @Test
    void suaSoLuong_donDatNguongMienPhi_phiVe0() {
        service.setCaiDatVanChuyen(CaiDatVanChuyenService.trongBoNho(true, 700_000L));
        service.suaDon(1, sua("Số 15 Trần Phú, Hà Đông, Hà Nội", new HoaDonSuaService.DongSua(1, 1, 3)));
        assertEquals(0, BigDecimal.ZERO.compareTo(hd.getPhiVanChuyen()), "750.000 đạt ngưỡng 700.000 nên miễn phí giao hàng");
        assertEquals(0, BigDecimal.valueOf(750_000).compareTo(hd.getThanhTien()));
        assertTrue(lichSu.get(0).getMoTa().contains("Phí giao hàng 30.000 ₫ → 0 ₫"));
    }

    @Test
    void phiSauKhiSua_cacTruongHop() {
        service.setCaiDatVanChuyen(CaiDatVanChuyenService.trongBoNho(true, 700_000L, 40_000L));
        String haNoi = "Số 15 Trần Phú, Hà Đông, Hà Nội";
        String hcm = "Số 8 Lê Duẩn, Phường Bến Nghé, Thành phố Hồ Chí Minh";
        assertEquals(30_000L, service.phiSauKhiSua(haNoi, haNoi, 900_000L, 900_000L, 30_000L),
                "không đổi tiền hàng (chỉ đổi tên người nhận): giữ phí của đơn dù đang trên ngưỡng");
        assertEquals(30_000L, service.phiSauKhiSua(hcm, haNoi, 500_000L, 500_000L, 30_000L), "phí không theo tỉnh: đổi địa chỉ thì giữ phí cũ");
        assertEquals(0L, service.phiSauKhiSua(hcm, haNoi, 900_000L, 500_000L, 30_000L), "đơn mới đạt ngưỡng: miễn phí");
        assertEquals(40_000L, service.phiSauKhiSua(hcm, hcm, 500_000L, 900_000L, 0L),
                "trước được miễn phí nhờ đạt ngưỡng, bớt hàng tụt dưới ngưỡng: phí quản lý đang cài");
        assertEquals(30_000L, service.phiSauKhiSua(hcm, hcm, 400_000L, 500_000L, 30_000L), "trước và sau đều dưới ngưỡng: giữ phí nhân viên đã nhập");
        service.setCaiDatVanChuyen(CaiDatVanChuyenService.trongBoNho(false, 700_000L));
        assertEquals(30_000L, service.phiSauKhiSua(haNoi, haNoi, 900_000L, 500_000L, 30_000L), "đang tắt miễn phí theo giá trị đơn: như cũ");
    }

    /** Thư "tạm hết hàng" đã gửi: [email, mã đơn, các món thiếu, số tiền đã chuyển khoản]. */
    private final List<Object[]> thuHetHang = new ArrayList<>();

    private void ganMailVaKhach() {
        KhachHang kh = new KhachHang();
        kh.setEmail("lan@gmail.com");
        hd.setKhachHang(kh);
        service.setMailService(new MailService(new MailServiceTest.NoiCap(null), "", "", "", "FootStyle", "") {
            @Override
            public void guiThongBaoHetHang(String hoTen, String email, String maHoaDon, List<String> monThieu, String daChuyenKhoan) {
                thuHetHang.add(new Object[] {email, maHoaDon, monThieu, daChuyenKhoan});
            }
        });
    }

    // Góp ý 14/11 (video 22:22 [12:42–14:03]): đơn online mà quầy vừa bán hết thì không hủy, giữ đơn và gửi thư chăm sóc khách.
    @Test
    void tienChuyenKhoanVeLucHetHang_khongHuyDon_ghiTien_guiThuChamSocKhach() {
        ganMailVaKhach();
        cau.setSoLuongTon(1); // đơn cần 2
        String kq = service.ghiNhanChuyenKhoanTuDong("HD01", 530_000, "FT-HH-1", "HD01");
        assertEquals(HoaDon.CHO_XAC_NHAN, hd.getTrangThai(), "đơn giữ Chờ xác nhận, không bị hủy");
        assertEquals(1, cau.getSoLuongTon(), "chưa trừ kho");
        assertEquals(1, thanhToan.size());
        assertEquals(0, BigDecimal.valueOf(530_000).compareTo(thanhToan.get(0).getSoTien()));
        assertTrue(lichSu.stream().anyMatch(l -> HoaDonSuaService.HANH_DONG_BAO_HET_HANG.equals(l.getHanhDong())));
        assertTrue(lichSu.stream().noneMatch(l -> "Đã hủy".equals(l.getHanhDong())));
        assertEquals(1, thuHetHang.size());
        assertEquals("lan@gmail.com", thuHetHang.get(0)[0]);
        assertTrue(String.valueOf(thuHetHang.get(0)[2]).contains("SP07-TRANG"));
        assertTrue(kq.contains("không hủy"), kq);
        // ngân hàng gửi lại cùng giao dịch: không ghi trùng, không gửi thêm thư
        service.ghiNhanChuyenKhoanTuDong("HD01", 530_000, "FT-HH-1", "HD01");
        assertEquals(1, thanhToan.size());
        assertEquals(1, thuHetHang.size());
    }

    @Test
    void nhanVienBaoHetHang_guiThu_ghiLichSu_donVanChoXacNhan_duHangThiBaoLoi() {
        ganMailVaKhach();
        assertThrows(ApiException.class, () -> service.baoHetHang(1), "đơn còn đủ hàng thì không gửi");
        assertEquals(0, thuHetHang.size());
        cau.setSoLuongTon(0);
        service.baoHetHang(1);
        assertEquals(HoaDon.CHO_XAC_NHAN, hd.getTrangThai());
        assertEquals(1, thuHetHang.size());
        assertEquals(null, thuHetHang.get(0)[3], "chưa chuyển khoản");
        LichSuHoaDon ls = lichSu.get(lichSu.size() - 1);
        assertEquals(HoaDonSuaService.HANH_DONG_BAO_HET_HANG, ls.getHanhDong());
        assertTrue(ls.getMoTa().contains("lan@gmail.com"), ls.getMoTa());
        hd.setTrangThai(HoaDon.DA_XAC_NHAN);
        assertThrows(ApiException.class, () -> service.baoHetHang(1), "chỉ đơn Chờ xác nhận");
    }

    // Góp ý 14/11: tách "Sửa thông tin giao hàng" khỏi "Chỉnh sửa đơn"; nhân viên và khách đều đổi được, mỗi đơn 1 lần
    private HoaDonSuaService.SuaGiaoHangRequest giao(String phuong, String tinh, String lyDo) {
        return new HoaDonSuaService.SuaGiaoHangRequest("Trần Văn Hùng", "0912 345 678", "Số 8 Lê Duẩn", phuong, tinh, lyDo);
    }

    @Test
    void nhanVienSuaGiaoHang_giuPhi_ghiLichSu_chiMotLan() {
        service.suaThongTinGiaoHang(1, giao("Phường Bến Nghé", "Thành phố Hồ Chí Minh", "Khách gọi đổi địa chỉ"));
        assertEquals("Số 8 Lê Duẩn, Phường Bến Nghé, Thành phố Hồ Chí Minh", hd.getDiaChiGiaoHang());
        assertEquals("Trần Văn Hùng", hd.getHoTenNguoiNhan());
        assertEquals("0912345678", hd.getSdtNguoiNhan());
        assertEquals(0, BigDecimal.valueOf(30_000).compareTo(hd.getPhiVanChuyen()), "phí không theo tỉnh: giữ 30.000");
        assertEquals(0, BigDecimal.valueOf(530_000).compareTo(hd.getThanhTien()));
        assertEquals(1, dong.size(), "sản phẩm giữ nguyên");
        assertEquals(2, dong.get(0).getSoLuong());
        LichSuHoaDon ls = lichSu.get(lichSu.size() - 1);
        assertEquals(HoaDonSuaService.HANH_DONG_SUA_GIAO, ls.getHanhDong());
        assertTrue(ls.getMoTa().contains("trước: Nguyễn Thị Lan"), ls.getMoTa());
        assertFalse(ls.getMoTa().contains("Phí giao hàng 30.000 ₫ →"), ls.getMoTa());
        ApiException e = assertThrows(ApiException.class, () -> service.suaThongTinGiaoHang(1, giao("Xã Sóc Sơn", "Thành phố Hà Nội", "Đổi lần 2")));
        assertEquals("DA_SUA_GIAO_HANG", e.getCode());
        // vẫn còn "Chỉnh sửa đơn" (sản phẩm) riêng
        service.suaDon(1, new HoaDonSuaService.SuaDonRequest("Trần Văn Hùng", "0912345678", hd.getDiaChiGiaoHang(),
                List.of(new HoaDonSuaService.DongSua(1, 1, 1)), "Khách bớt 1 ống cầu"));
        assertEquals(1, dong.get(0).getSoLuong());
    }

    @Test
    void khachSuaGiaoHang_khongCanLyDo_daTraTruoc_phiGiuNguyen_khongHoan() {
        daChuyenKhoan(530_000);
        service.khachSuaThongTinGiaoHang(1, giao("Phường Hà Đông", "Thành phố Hà Nội", null));
        assertEquals(0, BigDecimal.valueOf(30_000).compareTo(hd.getPhiVanChuyen()), "đổi địa chỉ không đổi phí");
        assertEquals(0, BigDecimal.valueOf(530_000).compareTo(hd.getThanhTien()));
        assertEquals(1, thanhToan.size(), "tổng không đổi nên không phát sinh hoàn tiền");
        assertEquals("Khách hàng", lichSu.get(lichSu.size() - 1).getNguoiThaoTac());
    }

    @Test
    void suaGiaoHang_kiemTraDuLieu_vaTrangThai() {
        LoiTruong loi = new LoiTruong(assertThrows(ApiException.class, () -> service.suaThongTinGiaoHang(1,
                new HoaDonSuaService.SuaGiaoHangRequest(" ", "123", "", "", "", ""))));
        assertTrue(loi.co("hoTenNguoiNhan") && loi.co("sdtNguoiNhan") && loi.co("tinhThanh") && loi.co("phuongXa") && loi.co("diaChiCuThe") && loi.co("lyDo"));
        assertThrows(ApiException.class, () -> service.suaThongTinGiaoHang(1,
                new HoaDonSuaService.SuaGiaoHangRequest("Nguyễn Thị Lan", "0901000001", "Số 15 Trần Phú", "Hà Đông", "Hà Nội", "x")), "chưa đổi gì");
        hd.setTrangThai(HoaDon.DANG_GIAO);
        assertThrows(ApiException.class, () -> service.suaThongTinGiaoHang(1, giao("Phường Bến Nghé", "Thành phố Hồ Chí Minh", "x")));
    }

    /** Đọc lỗi theo trường của ApiException (form). */
    private record LoiTruong(ApiException e) {
        boolean co(String truong) {
            return e.getLoiTheoTruong() != null && e.getLoiTheoTruong().containsKey(truong);
        }
    }

    /* ---------------- việc còn lại 15/11 ---------------- */

    // Cô review 14/11 [30:53] "tại sao lại là đã xác nhận?": tiền về chỉ ghi đã thanh toán, nhân viên mới xác nhận đơn
    @Test
    void tienChuyenKhoanVeDu_chiGhiDaThanhToan_khongTuXacNhan_khongTruKho() {
        String kq = service.ghiNhanChuyenKhoanTuDong("HD01", 530_000, "FT-OK-1", "HD01");
        assertEquals(HoaDon.CHO_XAC_NHAN, hd.getTrangThai(), "vẫn Chờ xác nhận");
        assertEquals(10, cau.getSoLuongTon(), "chưa trừ kho");
        assertEquals(1, thanhToan.size());
        assertTrue(lichSu.stream().anyMatch(l -> "Xác nhận thanh toán".equals(l.getHanhDong())));
        assertTrue(lichSu.stream().noneMatch(l -> "Đã xác nhận".equals(l.getHanhDong())), "không còn dòng HỆ THỐNG tự xác nhận");
        assertTrue(kq.contains("Đã ghi nhận"), kq);
    }

    // Ghi chú 28/10: sửa đơn làm đơn dưới mức tối thiểu thì bỏ phiếu VÀ trả lại lượt phiếu (như khi hủy đơn)
    @Test
    void suaDonDuoiMucToiThieu_boPhieu_traLaiLuot() {
        com.footstyle.demo.entity.PhieuGiamGia p = new com.footstyle.demo.entity.PhieuGiamGia();
        p.setId(77);
        p.setMaPhieu("PGG77");
        p.setLoaiGiam(2);
        p.setGiaTriGiam(50_000L);
        p.setGiaTriDonHangToiThieu(400_000L);
        hd.setPhieuGiamGia(p);
        hd.setSoTienGiam(BigDecimal.valueOf(50_000));
        hd.setThanhTien(BigDecimal.valueOf(480_000));
        service.suaDon(1, sua("Số 15 Trần Phú, Hà Đông, Hà Nội", new HoaDonSuaService.DongSua(1, 1, 1)));
        assertNull(hd.getPhieuGiamGia());
        assertEquals(List.of(77), phieuTraLuot, "trả lại 1 lượt phiếu PGG77");
        assertTrue(lichSu.get(lichSu.size() - 1).getMoTa().contains("Trả lại lượt phiếu PGG77"), lichSu.get(lichSu.size() - 1).getMoTa());
    }

    // Cô test case khó 14/11 [08:21]: xác nhận đã hoàn tiền thì khách nhận được email
    @Test
    void xacNhanHoanTien_guiThuDaHoanTienChoKhach() {
        List<String[]> thu = new ArrayList<>();
        KhachHang kh = new KhachHang();
        kh.setEmail("lan@gmail.com");
        hd.setKhachHang(kh);
        service.setMailService(new MailService(new MailServiceTest.NoiCap(null), "", "", "", "FootStyle", "") {
            @Override
            public void guiDaHoanTien(String hoTen, String email, String maHoaDon, String soTien, String maGiaoDich) {
                thu.add(new String[] {email, maHoaDon, soTien, maGiaoDich});
            }
        });
        daChuyenKhoan(530_000);
        hd.setTrangThai(HoaDon.DA_HUY);
        service.xacNhanHoanTien(1, new HoaDonSuaService.XacNhanThanhToanRequest("FT-HOAN-9"));
        assertEquals(HoaDon.HOAN_TIEN, hd.getTrangThai());
        assertEquals(1, thu.size());
        assertEquals("lan@gmail.com", thu.get(0)[0]);
        assertEquals("530.000 ₫", thu.get(0)[2]);
        assertEquals("FT-HOAN-9", thu.get(0)[3]);
        assertTrue(lichSu.get(lichSu.size() - 1).getMoTa().contains("Đã gửi thư báo tới lan@gmail.com"));
    }
}
