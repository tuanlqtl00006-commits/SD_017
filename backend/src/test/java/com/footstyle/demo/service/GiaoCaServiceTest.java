package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.entity.GiaoCa;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.security.XacThuc;
import java.math.BigDecimal;
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

    private Map<String, Object> nhanCa(String gio, long tienDau, String ghiChu) {
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
    void nhanCaSom_vanTinhLaCaDo_khongMuon() {
        m.lich(m.bao, m.sang, CaLamMau.HOM_NAY);
        CaLamMau.dangNhap(m.bao);
        Map<String, Object> gc = nhanCa("06:10", 0, null);
        assertEquals("Ca sáng", gc.get("tenCa"));
        assertEquals(0, gc.get("soPhutDiMuon"));
        assertEquals(false, gc.get("diMuon"));
    }

    @Test
    void khongCoLich_vanNhanDuoc_ghiNgoaiLich() {
        CaLamMau.dangNhap(m.anh);
        Map<String, Object> gc = nhanCa("09:00", 500_000, null);
        assertEquals(true, gc.get("ngoaiLich"));
        assertNull(gc.get("soPhutDiMuon"));
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
        CaLamMau.dangNhap(m.bao);
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
        // ca sau gợi ý tiền đầu ca = 1.000.000 vừa bàn giao
        CaLamMau.dangNhap(m.anh);
        assertEquals(tien(1_000_000), service.hienTai().get("goiYTienDauCa"));
        assertEquals(HttpStatus.CONFLICT, loi(() -> service.ketCa(new GiaoCaService.KetCaRequest(tien(0), tien(0), null, null))).getStatus());
    }

    @Test
    void ketCa_thuaTien_coGhiChuThiDuoc() {
        CaLamMau.dangNhap(m.bao);
        nhanCa("07:00", 0, null);
        Map<String, Object> xong = service.ketCa(new GiaoCaService.KetCaRequest(tien(20_000), tien(0), null, "Khách không lấy tiền thừa"));
        assertEquals(tien(20_000), xong.get("chenhLech"));
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
        CaLamMau.dangNhap(m.bao);
        nhanCa("07:00", 0, null);
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

    @Test
    void chuaDangNhap_401() {
        assertEquals(HttpStatus.UNAUTHORIZED, loi(service::hienTai).getStatus());
    }
}
