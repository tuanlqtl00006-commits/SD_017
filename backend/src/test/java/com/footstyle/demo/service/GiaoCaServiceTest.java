package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.entity.GiaoCa;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.NhanVien;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.security.NguoiDung;
import com.footstyle.demo.security.XacThuc;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GiaoCaServiceTest {

    private final CaLamMau m = new CaLamMau();
    private final GiaoCaService service = new GiaoCaService(m.giaoCaRepo(), m.nhanVienRepo(), m.lichRepo(), m.hoaDonRepo(),
            m.thanhToanRepo(), m.phuongThucRepo());

    @AfterEach
    void don() {
        XacThuc.xoa();
    }

    private static ApiException loi(Runnable r) {
        return assertThrows(ApiException.class, r::run);
    }

    private static BigDecimal tien(long v) {
        return BigDecimal.valueOf(v);
    }

    /** Nhân viên chỉ nhận ca theo lịch: người đang đăng nhập chưa có lịch hôm nay thì xếp cho ca đang diễn ra lúc `gio`. */
    private void xepLichNeuChua(String gio) {
        NguoiDung nd = XacThuc.nhanVien();
        if (nd.laQuanLy()) return;
        NhanVien nv = m.nhanViens.stream().filter(n -> n.getId().equals(nd.id())).findFirst().orElseThrow();
        if (m.lichs.stream().anyMatch(l -> l.getNhanVien() == nv && l.getNgayLam().equals(CaLamMau.HOM_NAY))) return;
        LocalTime t = LocalTime.parse(gio);
        m.lich(nv, t.isBefore(LocalTime.of(11, 45)) ? m.sang : t.isBefore(LocalTime.of(16, 45)) ? m.chieu : m.toi, CaLamMau.HOM_NAY);
    }

    private Map<String, Object> nhanCa(String gio, long tienDau, String ghiChu) {
        xepLichNeuChua(gio);
        service.datDongHo(CaLamMau.luc(CaLamMau.HOM_NAY, gio));
        return service.nhanCa(new GiaoCaService.NhanCaRequest(tien(tienDau), ghiChu, null, tien(0)));
    }

    @Test
    void nhanCa_tuGanCaTrongLich_tinhPhutMuon() {
        m.lich(m.bao, m.sang, CaLamMau.HOM_NAY);
        CaLamMau.dangNhap(m.bao);
        Map<String, Object> gc = nhanCa("07:20", 1_000_000, null);
        assertEquals("GC01", gc.get("maGiaoCa"));
        assertEquals("Ca sáng", gc.get("tenCa"));
        assertEquals("07:00 - 12:00", gc.get("gioCa"));
        assertEquals(20, gc.get("soPhutDiMuon"));
        assertEquals(true, gc.get("diMuon"), "muộn quá 15 phút");
        assertEquals(false, gc.get("ngoaiLich"));
    }

    @Test
    void nhanCaSomToiDa15Phut_vanTinhLaCaDo_khongMuon() {
        m.lich(m.bao, m.sang, CaLamMau.HOM_NAY);
        CaLamMau.dangNhap(m.bao);
        Map<String, Object> gc = nhanCa("06:45", 0, null);
        assertEquals("Ca sáng", gc.get("tenCa"));
        assertEquals(0, gc.get("soPhutDiMuon"));
        assertEquals(false, gc.get("diMuon"));
    }

    @Test
    void nhanCaSomHon15Phut_chonCa_baoGio_tuDongCungBiChan_vaVaoDuocKhiDenGio() {
        Integer idLich = m.lich(m.bao, m.sang, CaLamMau.HOM_NAY).getId();
        CaLamMau.dangNhap(m.bao);
        service.datDongHo(CaLamMau.luc(CaLamMau.HOM_NAY, "06:30"));
        ApiException e = loi(() -> service.nhanCa(new GiaoCaService.NhanCaRequest(tien(0), null, idLich, tien(0))));
        assertTrue(e.getLoiTheoTruong().get("idLichLamViec").contains("từ 06:45"));
        // tự chọn theo giờ mà quá sớm: hôm nay có lịch nên không được vào "ngoài lịch"
        ApiException tuDong = loi(() -> nhanCa("06:30", 0, null));
        assertEquals("NGOAI_LICH_BI_CHAN", tuDong.getCode());
        assertTrue(tuDong.getMessage().contains("Ca sáng 07:00 - 12:00") && tuDong.getMessage().contains("từ 15 phút trước"), tuDong.getMessage());
        assertTrue(m.giaoCas.isEmpty());
        assertEquals("Ca sáng", nhanCa("06:50", 0, null).get("tenCa"), "đến giờ thì vào được");
    }

    @Test
    void coLichCaToi_dangNhapSang_khongVaoNgoaiLichDuoc() {
        m.lich(m.anh, m.toi, CaLamMau.HOM_NAY);
        CaLamMau.dangNhap(m.anh);
        ApiException e = loi(() -> nhanCa("07:35", 0, null));
        assertEquals("NGOAI_LICH_BI_CHAN", e.getCode());
        assertTrue(e.getMessage().contains(m.toi.getTenCa()) && e.getMessage().contains("quản lý xếp thêm lịch"), e.getMessage());
        assertEquals("Ca tối", nhanCa("17:00", 0, null).get("tenCa"));
    }

    @Test
    void khongCoLich_khongNhanCaDuoc_ngayLanDem_trangVaoCaBietTruocLyDo() {
        CaLamMau.dangNhap(m.anh);
        for (String gio : new String[] {"09:00", "21:30", "23:15"}) {
            service.datDongHo(CaLamMau.luc(CaLamMau.HOM_NAY, gio));
            ApiException e = loi(() -> service.nhanCa(new GiaoCaService.NhanCaRequest(tien(500_000), null, null, tien(0))));
            assertEquals("NGOAI_LICH_BI_CHAN", e.getCode(), gio);
            assertTrue(e.getMessage().startsWith("Hôm nay bạn không có lịch làm nên không nhận ca được."), e.getMessage());
            assertEquals(e.getMessage(), service.hienTai().get("lyDoChuaNhanCa"), "trang Vào ca nhận sẵn lý do để khóa nút");
        }
        assertTrue(m.giaoCas.isEmpty());
        // Có lịch ca tối: 21:30 nhận được (lý do = null), 22:00 hết giờ ca thì không
        m.lich(m.anh, m.toi, CaLamMau.HOM_NAY);
        service.datDongHo(CaLamMau.luc(CaLamMau.HOM_NAY, "21:30"));
        assertNull(service.hienTai().get("lyDoChuaNhanCa"));
        service.datDongHo(CaLamMau.luc(CaLamMau.HOM_NAY, "22:00"));
        assertTrue(String.valueOf(service.hienTai().get("lyDoChuaNhanCa")).contains("Ca tối"));
        assertEquals("NGOAI_LICH_BI_CHAN", loi(() -> service.nhanCa(new GiaoCaService.NhanCaRequest(tien(0), null, null, tien(0)))).getCode());
    }

    @Test
    void dangTrongCa_khongNhanThemDuoc() {
        CaLamMau.dangNhap(m.bao);
        nhanCa("07:00", 0, null);
        ApiException e = loi(() -> nhanCa("08:00", 0, null));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        assertEquals("DANG_TRONG_CA", e.getCode());
    }

    @Test
    void tienDauCa_kiemTra() {
        CaLamMau.dangNhap(m.bao);
        xepLichNeuChua("07:00");
        service.datDongHo(CaLamMau.luc(CaLamMau.HOM_NAY, "07:00"));
        for (BigDecimal sai : new BigDecimal[] {null, tien(-1), new BigDecimal("100.5"), tien(1_000_000_001L)}) {
            ApiException e = loi(() -> service.nhanCa(new GiaoCaService.NhanCaRequest(sai, null, null)));
            assertTrue(e.getLoiTheoTruong().containsKey("tienDauCa"), String.valueOf(sai));
        }
        assertEquals("GC01", service.nhanCa(new GiaoCaService.NhanCaRequest(new BigDecimal("200000.00"), null, null)).get("maGiaoCa"));
    }

    private void caTruocBanGiao(long tienBanGiao) {
        GiaoCa truoc = new GiaoCa();
        truoc.setId(m.giaoCas.size() + 1);
        truoc.setMaGiaoCa("GC0" + truoc.getId());
        truoc.setNhanVien(m.anh);
        truoc.setThoiGianNhanCa(CaLamMau.HOM_NAY.minusDays(1).atTime(17, 0));
        truoc.setThoiGianKetCa(CaLamMau.HOM_NAY.minusDays(1).atTime(22, 0));
        truoc.setTienDauCa(tien(0));
        truoc.setTienBanGiao(tien(tienBanGiao));
        truoc.setTrangThai(GiaoCa.DA_GIAO_CA);
        m.giaoCas.add(truoc);
    }

    @Test
    void tienDauCaKhacTienCaTruocBanGiao_batBuocGhiChu() {
        caTruocBanGiao(2_000_000);
        CaLamMau.dangNhap(m.bao);
        service.datDongHo(CaLamMau.luc(CaLamMau.HOM_NAY, "07:00"));
        Map<String, Object> ht = service.hienTai();
        assertEquals(tien(2_000_000), ht.get("goiYTienDauCa"));
        assertEquals("Phạm Minh Anh", ht.get("nguoiBanGiao"));
        ApiException e = loi(() -> nhanCa("07:00", 1_500_000, "  "));
        assertTrue(e.getLoiTheoTruong().get("ghiChu").contains("2.000.000 ₫"));
        assertEquals("GC02", nhanCa("07:00", 1_500_000, "Ca trước lấy 500k mua đồ").get("maGiaoCa"));
    }

    @Test
    void vaoCa_phaiXacNhanChuyenKhoanCaTruoc_lechThiGhiLyDo() {
        caTruocBanGiao(2_000_000);
        m.giaoCas.get(m.giaoCas.size() - 1).setChuyenKhoanThu(tien(1_200_000));
        CaLamMau.dangNhap(m.bao);
        xepLichNeuChua("07:00");
        service.datDongHo(CaLamMau.luc(CaLamMau.HOM_NAY, "07:00"));
        assertEquals(tien(1_200_000), service.hienTai().get("chuyenKhoanCaTruoc"));
        ApiException e = loi(() -> service.nhanCa(new GiaoCaService.NhanCaRequest(tien(2_000_000), null, null, null)));
        assertTrue(e.getLoiTheoTruong().containsKey("chuyenKhoanCaTruoc"), "có ca trước thì bắt buộc nhập chuyển khoản");
        e = loi(() -> service.nhanCa(new GiaoCaService.NhanCaRequest(tien(2_000_000), null, null, tien(1_000_000))));
        assertTrue(e.getLoiTheoTruong().get("ghiChu").contains("1.200.000 ₫"));
        assertEquals("GC02", service.nhanCa(new GiaoCaService.NhanCaRequest(tien(2_000_000), null, null, tien(1_200_000))).get("maGiaoCa"));
    }

    @Test
    void tienDauCaDungTienBanGiao_khongCanGhiChu() {
        caTruocBanGiao(2_000_000);
        CaLamMau.dangNhap(m.bao);
        assertEquals("GC02", nhanCa("07:00", 2_000_000, null).get("maGiaoCa"));
    }

    @Test
    void tongKet_tachTienMatChuyenKhoan_boDonHuy() {
        CaLamMau.dangNhap(m.bao);
        nhanCa("07:00", 1_000_000, null);
        int id = m.giaoCas.get(0).getId();
        m.hoaDon(id, 1_000_000, HoaDon.HOAN_THANH, "PT01", 1_000_000);
        m.hoaDon(id, 500_000, HoaDon.HOAN_THANH, "PT02", 500_000);
        m.hoaDon(id, 300_000, HoaDon.HOAN_THANH, "PT01", 100_000, "PT02", 200_000);
        m.hoaDon(id, 900_000, HoaDon.DA_HUY, "PT01", 900_000);
        m.hoaDon(null, 700_000, HoaDon.HOAN_THANH, "PT01", 700_000); // đơn khác ca
        Map<String, Object> tk = service.tongKet(m.giaoCas.get(0));
        assertEquals(3, tk.get("soHoaDon"));
        assertEquals(tien(1_800_000), tk.get("doanhThu"));
        assertEquals(tien(1_100_000), tk.get("tienMat"));
        assertEquals(tien(700_000), tk.get("chuyenKhoan"));
        assertEquals(tien(2_100_000), tk.get("tienMatHeThong"));
    }

    @Test
    void ketCa_lechTienBatBuocGhiLyDo_banGiaoKhongQuaThucTe() {
        CaLamMau.dangNhapThuNgan(m.bao);
        nhanCa("07:00", 1_000_000, null);
        int id = m.giaoCas.get(0).getId();
        m.hoaDon(id, 400_000, HoaDon.HOAN_THANH, "PT01", 400_000);
        service.datDongHo(CaLamMau.luc(CaLamMau.HOM_NAY, "12:05"));
        ApiException e = loi(() -> service.ketCa(new GiaoCaService.KetCaRequest(tien(1_350_000), tien(1_000_000), null, null)));
        assertTrue(e.getLoiTheoTruong().get("ghiChu").contains("thiếu 50.000 ₫"));
        e = loi(() -> service.ketCa(new GiaoCaService.KetCaRequest(tien(1_400_000), tien(1_500_000), null, null)));
        assertEquals("Tiền bàn giao không được lớn hơn tiền mặt thực tế.", e.getLoiTheoTruong().get("tienBanGiao"));
        e = loi(() -> service.ketCa(new GiaoCaService.KetCaRequest(null, null, 3, null)));
        assertTrue(e.getLoiTheoTruong().keySet().containsAll(List.of("tienMatThucTe", "tienBanGiao", "idNhanVienNhanBanGiao")));
        e = loi(() -> service.ketCa(new GiaoCaService.KetCaRequest(tien(1_400_000), tien(1_000_000), 10, null)));
        assertTrue(e.getLoiTheoTruong().get("idNhanVienNhanBanGiao").contains("ngừng hoạt động"));

        Map<String, Object> xong = service.ketCa(new GiaoCaService.KetCaRequest(tien(1_400_000), tien(1_000_000), 4, null));
        assertEquals(false, xong.get("dangTrongCa"));
        assertEquals(tien(1_400_000), xong.get("tienMatHeThong"));
        assertEquals(0, ((BigDecimal) xong.get("chenhLech")).signum());
        assertEquals("Phạm Minh Anh", xong.get("tenNguoiNhanBanGiao"));
        assertEquals(1, ((List<?>) xong.get("hoaDons")).size());
        // ca sau gợi ý tiền đầu ca = 1.000.000 vừa bàn giao; Thu ngân chưa có ca nào đang mở thì không chốt được
        CaLamMau.dangNhapThuNgan(m.anh);
        assertEquals(tien(1_000_000), service.hienTai().get("goiYTienDauCa"));
        assertEquals(HttpStatus.CONFLICT, loi(() -> service.ketCa(new GiaoCaService.KetCaRequest(tien(0), tien(0), null, null))).getStatus());
    }

    /** Mỗi ca một nhân viên làm hết: nhân viên thường (không cần vai trò Thu ngân) tự giao ca của mình; ca của người khác thì không. */
    @Test
    void nhanVienTuGiaoCaCuaMinh_khongGiaoHoCaNguoiKhac() {
        CaLamMau.dangNhap(m.bao);
        nhanCa("07:00", 500_000, null);
        CaLamMau.dangNhap(m.anh);
        assertEquals("CHUA_NHAN_CA", loi(() -> service.ketCa(new GiaoCaService.KetCaRequest(tien(500_000), tien(500_000), null, null))).getCode());
        assertEquals("CHUA_NHAN_CA", loi(() -> service.caDangMoBatBuoc(m.anh.getId())).getCode(), "không bán nhờ ca của người khác");
        CaLamMau.dangNhap(m.bao);
        Map<String, Object> xong = service.ketCa(new GiaoCaService.KetCaRequest(tien(500_000), tien(500_000), null, null));
        assertEquals(false, xong.get("dangTrongCa"));
    }

    @Test
    void ketCa_thuaTien_coGhiChuThiDuoc() {
        CaLamMau.dangNhapThuNgan(m.bao);
        nhanCa("07:00", 0, null);
        Map<String, Object> xong = service.ketCa(new GiaoCaService.KetCaRequest(tien(20_000), tien(0), null, "Khách không lấy tiền thừa"));
        assertEquals(tien(20_000), xong.get("chenhLech"));
    }

    @Test
    void motKetTien_caKhacDangMo_khongVaoCaDuoc_hienTaiBaoTruoc() {
        caTruocBanGiao(2_000_000);
        CaLamMau.dangNhap(m.bao);
        nhanCa("07:00", 2_000_000, null);
        // người thứ hai vào ca khi ca của Bảo chưa giao: bị chặn, trang Vào ca được báo trước
        CaLamMau.dangNhap(m.anh);
        service.datDongHo(CaLamMau.luc(CaLamMau.HOM_NAY, "12:05"));
        @SuppressWarnings("unchecked")
        Map<String, Object> caKhac = (Map<String, Object>) service.hienTai().get("caKhacDangMo");
        assertEquals("GC02", caKhac.get("maGiaoCa"));
        ApiException e = loi(() -> service.nhanCa(new GiaoCaService.NhanCaRequest(tien(2_000_000), null, null, tien(0))));
        assertEquals("CA_KHAC_DANG_MO", e.getCode());
        assertTrue(e.getMessage().contains("GC02") && e.getMessage().contains("Lê Quốc Bảo"), e.getMessage());
        // Bảo giao ca xong thì Ánh vào được, nhận đúng tiền Bảo để lại
        CaLamMau.dangNhapThuNgan(m.bao);
        service.ketCa(new GiaoCaService.KetCaRequest(tien(2_000_000), tien(2_000_000), null, null));
        CaLamMau.dangNhap(m.anh);
        assertNull(service.hienTai().get("caKhacDangMo"));
        assertEquals(tien(2_000_000), service.hienTai().get("goiYTienDauCa"));
        assertEquals("GC03", nhanCa("12:05", 2_000_000, null).get("maGiaoCa"));
    }

    @Test
    void quanLyDongCaHo_nguoiDaVeQuenGiao_caSauVaoDuoc() {
        CaLamMau.dangNhap(m.bao);
        nhanCa("07:00", 1_000_000, null);
        int id = m.giaoCas.get(0).getId();
        GiaoCaService.KetCaRequest dem = new GiaoCaService.KetCaRequest(tien(1_000_000), tien(1_000_000), null, null);
        // nhân viên khác không đóng hộ được
        CaLamMau.dangNhap(m.anh);
        assertEquals(HttpStatus.FORBIDDEN, loi(() -> service.dongCaHo(id, dem)).getStatus());
        // quản lý đóng hộ: ghi rõ ai đóng hộ, ca chuyển sang đã giao
        CaLamMau.dangNhap(m.quanLy);
        service.datDongHo(CaLamMau.luc(CaLamMau.HOM_NAY, "13:00"));
        Map<String, Object> xong = service.dongCaHo(id, dem);
        assertEquals(false, xong.get("dangTrongCa"));
        assertTrue(String.valueOf(xong.get("ghiChuKetCa")).startsWith("Quản lý " + m.quanLy.getHoTen() + " đóng ca hộ lúc 13:00"), String.valueOf(xong.get("ghiChuKetCa")));
        assertEquals("CA_DA_GIAO", loi(() -> service.dongCaHo(id, dem)).getCode());
        // không bàn giao cho chính người trong ca
        CaLamMau.dangNhap(m.anh);
        nhanCa("13:05", 1_000_000, null);
        int idAnh = m.giaoCas.get(1).getId();
        CaLamMau.dangNhap(m.quanLy);
        ApiException e = loi(() -> service.dongCaHo(idAnh, new GiaoCaService.KetCaRequest(tien(1_000_000), tien(0), m.anh.getId(), null)));
        assertTrue(e.getLoiTheoTruong().get("idNhanVienNhanBanGiao").contains("chính người trong ca"));
    }

    @Test
    void caCuTuHomTruocChuaGiao_khongBanHangDuoc() {
        CaLamMau.dangNhap(m.bao);
        nhanCa("21:00", 0, null);
        assertEquals("GC01", service.caDangMoBatBuoc(3).getMaGiaoCa());
        service.datDongHo(CaLamMau.luc(CaLamMau.HOM_NAY.plusDays(1), "08:00"));
        ApiException e = loi(() -> service.caDangMoBatBuoc(3));
        assertEquals("CA_CU_CHUA_GIAO", e.getCode());
        assertTrue(e.getMessage().contains("GC01"));
    }

    @Test
    void tongKet_doanhThuKhongGomPhiGiaoHang() {
        CaLamMau.dangNhap(m.bao);
        nhanCa("07:00", 0, null);
        int id = m.giaoCas.get(0).getId();
        m.hoaDon(id, 530_000, HoaDon.CHO_LAY_HANG, "PT01", 530_000).setPhiVanChuyen(tien(30_000));
        Map<String, Object> tk = service.tongKet(m.giaoCas.get(0));
        assertEquals(tien(500_000), tk.get("doanhThu"));
        assertEquals(tien(530_000), tk.get("tienMat"), "tiền mặt vẫn đủ số khách trả, gồm cả phí giao");
    }

    @Test
    void banHangBatBuocDangTrongCa() {
        ApiException e = loi(() -> service.caDangMoBatBuoc(3));
        assertEquals("CHUA_NHAN_CA", e.getCode());
        CaLamMau.dangNhap(m.bao);
        nhanCa("07:00", 0, null);
        assertEquals("GC01", service.caDangMoBatBuoc(3).getMaGiaoCa());
    }

    @Test
    void lichSu_nhanVienChiThayCaCuaMinh_quanLyThayHet() {
        CaLamMau.dangNhapThuNgan(m.bao);
        nhanCa("07:00", 0, null);
        service.ketCa(new GiaoCaService.KetCaRequest(tien(0), tien(0), null, null));
        CaLamMau.dangNhap(m.anh);
        nhanCa("12:00", 0, null);
        CaLamMau.dangNhap(m.bao);
        assertEquals(1, service.danhSach(null, null, 4).size(), "nhân viên truyền id người khác vẫn chỉ ra ca của mình");
        assertEquals("NV03", service.danhSach(null, null, null).get(0).get("maNhanVien"));
        int idCuaAnh = m.giaoCas.get(1).getId();
        assertEquals(HttpStatus.FORBIDDEN, loi(() -> service.chiTiet(idCuaAnh)).getStatus());
        CaLamMau.dangNhap(m.quanLy);
        assertEquals(2, service.danhSach(null, null, null).size());
        assertEquals(1, service.danhSach(null, null, 4).size());
        assertEquals("NV04", service.chiTiet(idCuaAnh).get("maNhanVien"));
    }

    /** Review: lịch sử giao ca cho biết nhân viên giữ ca đến hết rồi ca tiếp theo đến ai nhận. */
    @Test
    void lichSu_moiCaDaGiaoBietCaSauDoAiNhan() {
        CaLamMau.dangNhapThuNgan(m.bao);
        nhanCa("07:00", 0, null);
        service.ketCa(new GiaoCaService.KetCaRequest(tien(0), tien(0), null, null));
        CaLamMau.dangNhap(m.anh);
        nhanCa("12:00", 0, null);
        CaLamMau.dangNhap(m.quanLy);
        List<Map<String, Object>> ds = service.danhSach(null, null, null);
        Map<String, Object> caAnh = ds.get(0);
        Map<String, Object> caBao = ds.get(1);
        assertEquals("NV03", caBao.get("maNhanVien"));
        @SuppressWarnings("unchecked") Map<String, Object> sau = (Map<String, Object>) caBao.get("caSau");
        assertEquals("NV04", sau.get("maNhanVien"), "ca của Bảo giao xong thì Anh nhận tiếp");
        assertEquals(m.giaoCas.get(1).getMaGiaoCa(), sau.get("maGiaoCa"));
        assertNull(caAnh.get("caSau"), "ca đang mở chưa có ai nhận tiếp");
        // lọc riêng Bảo vẫn biết người nhận tiếp (ca của người khác)
        @SuppressWarnings("unchecked") Map<String, Object> sauLoc = (Map<String, Object>) service.danhSach(null, null, 3).get(0).get("caSau");
        assertEquals("NV04", sauLoc.get("maNhanVien"));
        @SuppressWarnings("unchecked") Map<String, Object> sauChiTiet = (Map<String, Object>) service.chiTiet(m.giaoCas.get(0).getId()).get("caSau");
        assertEquals("NV04", sauChiTiet.get("maNhanVien"));
    }

    @Test
    void caDaGiao_khongVaoLaiDuoc_duLaTuGiaoHayQuanLyDongHo() {
        Integer idLich = m.lich(m.bao, m.sang, CaLamMau.HOM_NAY).getId();
        CaLamMau.dangNhapThuNgan(m.bao);
        assertEquals(16, nhanCa("07:16", 1_000_000, null).get("soPhutDiMuon"));
        service.ketCa(new GiaoCaService.KetCaRequest(tien(1_000_000), tien(1_000_000), null, null));
        // tự giao rồi vào lại: cả chọn đúng ca lẫn tự chọn theo giờ đều bị chặn, không ghi thêm "muộn"
        service.datDongHo(CaLamMau.luc(CaLamMau.HOM_NAY, "07:47"));
        ApiException chon = loi(() -> service.nhanCa(new GiaoCaService.NhanCaRequest(tien(1_000_000), null, idLich, tien(0))));
        assertTrue(chon.getLoiTheoTruong().get("idLichLamViec").contains("đã giao rồi"));
        ApiException tuDong = loi(() -> nhanCa("07:47", 1_000_000, null));
        assertEquals("NGOAI_LICH_BI_CHAN", tuDong.getCode());
        assertTrue(tuDong.getMessage().contains("đã giao rồi"), tuDong.getMessage());
        assertEquals(1, m.giaoCas.size(), "không có ca mới nào được tạo");
    }

    @Test
    void quanLyDongCaHo_nhanVienDangNhapLai_khongTuVaoLaiCaDo() {
        m.lich(m.bao, m.sang, CaLamMau.HOM_NAY);
        CaLamMau.dangNhap(m.bao);
        nhanCa("07:16", 1_000_000, null);
        int id = m.giaoCas.get(0).getId();
        CaLamMau.dangNhap(m.quanLy);
        service.dongCaHo(id, new GiaoCaService.KetCaRequest(tien(1_000_000), tien(1_000_000), null, null));
        CaLamMau.dangNhap(m.bao);
        ApiException e = loi(() -> nhanCa("08:00", 1_000_000, null));
        assertEquals("NGOAI_LICH_BI_CHAN", e.getCode());
        // trang Vào ca biết ca nào đã giao để khóa lại
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lich = (List<Map<String, Object>>) service.hienTai().get("lichHomNay");
        assertEquals(true, lich.get(0).get("daGiao"));
        // quản lý xếp thêm lịch (ca chiều) thì nhân viên nhận được ca đó
        m.lich(m.bao, m.chieu, CaLamMau.HOM_NAY);
        assertEquals("Ca chiều", nhanCa("12:00", 1_000_000, null).get("tenCa"));
    }

    @Test
    void quanLyKhongVaoCa() {
        CaLamMau.dangNhap(m.quanLy);
        ApiException e = loi(() -> nhanCa("07:00", 0, null));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
        assertTrue(m.giaoCas.isEmpty());
    }

    @Test
    void chuaDangNhap_401() {
        assertEquals(HttpStatus.UNAUTHORIZED, loi(service::hienTai).getStatus());
    }
}
