package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.dto.HoaDonRequest;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.entity.LichSuHoaDon;
import com.footstyle.demo.entity.PhieuGiamGia;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

/**
 * Dữ liệu chi tiết / danh sách hóa đơn cho bản in và màn hình, theo lời cô:
 * "tại quầy sao lại có phí vận chuyển 30 nghìn", "khách lẻ tại sao có số điện thoại", "giảm dùng mã nào, giảm bao nhiêu phần trăm".
 */
class HoaDonChiTietHienThiTest {

    private final HoaDonKhoGia kho = new HoaDonKhoGia();
    private final HoaDon hd = kho.hd;
    private final HoaDonService service = kho.service;

    @SuppressWarnings("unchecked")
    private Map<String, Object> muc(String ten) {
        return (Map<String, Object>) service.getChiTietHoaDon(1).get(ten);
    }

    private static KhachHang khach(String ten, String sdt, String email) {
        KhachHang k = new KhachHang();
        k.setMaKhachHang("KH01");
        k.setHoTen(ten);
        k.setSdt(sdt);
        k.setEmail(email);
        return k;
    }

    private static PhieuGiamGia phieu(String ma, int loai, long giaTri, Long toiDa) {
        PhieuGiamGia p = new PhieuGiamGia();
        p.setMaPhieu(ma);
        p.setTenPhieu("Phiếu " + ma);
        p.setLoaiGiam(loai);
        p.setGiaTriGiam(giaTri);
        p.setGiamToiDa(toiDa);
        return p;
    }

    /* ---------- đơn tại quầy: không vận chuyển ---------- */

    @Test
    void donTaiQuay_phiVanChuyenLuonLa0_khongDonViVanChuyen_dauDuLieuCuConSot30000() {
        hd.setLoaiDon(HoaDon.TAI_QUAY);
        hd.setTongTien(new BigDecimal("500000"));
        hd.setSoTienGiam(new BigDecimal("50000"));
        hd.setPhiVanChuyen(new BigDecimal("30000"));          // nhập sai: đơn tại quầy mà có phí
        hd.setThanhTien(new BigDecimal("480000"));            // 500.000 - 50.000 + 30.000
        hd.setDonViVanChuyen("Giao Hàng Nhanh");

        Map<String, Object> info = muc("hoaDon");
        assertEquals(0, BigDecimal.ZERO.compareTo((BigDecimal) info.get("phiVanChuyen")));
        assertNull(info.get("donViVanChuyen"));
        assertEquals(0, new BigDecimal("450000").compareTo((BigDecimal) info.get("thanhTien")), "500.000 - 50.000 + 0");
        assertEquals("Tại quầy", info.get("tenLoaiDon"));
    }

    @Test
    void donTaiQuay_trongDanhSachCungKhongTinhPhiVanChuyen() {
        hd.setLoaiDon(HoaDon.TAI_QUAY);
        hd.setPhiVanChuyen(new BigDecimal("30000"));
        hd.setThanhTien(new BigDecimal("530000"));
        HoaDonRequest dong = service.getDanhSachCoLoc(null, null, null, null, null, PageRequest.of(0, 5)).getContent().get(0);
        assertEquals(0, new BigDecimal("500000").compareTo(dong.getTongTien()));
    }

    @Test
    void donTaiQuay_khongCoNguoiNhanVaDiaChiGiao() {
        hd.setLoaiDon(HoaDon.TAI_QUAY);
        hd.setHoTenNguoiNhan("Nguyễn Văn A");
        hd.setSdtNguoiNhan("0912345678");
        hd.setDiaChiGiaoHang("Số 1 Hà Nội");
        Map<String, Object> giao = muc("giaoHang");
        assertNull(giao.get("hoTenNguoiNhan"));
        assertNull(giao.get("sdtNguoiNhan"));
        assertNull(giao.get("diaChi"));
    }

    @Test
    void donGiaoHang_giuNguyenPhiVaDonViVanChuyen() {
        hd.setLoaiDon(HoaDon.GIAO_HANG);
        hd.setPhiVanChuyen(new BigDecimal("30000"));
        hd.setThanhTien(new BigDecimal("530000"));
        hd.setDonViVanChuyen("Giao Hàng Tiết Kiệm");
        hd.setHoTenNguoiNhan("Lê Minh Tuấn");
        hd.setSdtNguoiNhan("0901000003");
        hd.setDiaChiGiaoHang("Số 88 Lê Lợi");
        Map<String, Object> info = muc("hoaDon");
        assertEquals(0, new BigDecimal("30000").compareTo((BigDecimal) info.get("phiVanChuyen")));
        assertEquals("Giao Hàng Tiết Kiệm", info.get("donViVanChuyen"));
        assertEquals(0, new BigDecimal("530000").compareTo((BigDecimal) info.get("thanhTien")));
        assertEquals("Lê Minh Tuấn", muc("giaoHang").get("hoTenNguoiNhan"));
        assertEquals("Số 88 Lê Lợi", muc("giaoHang").get("diaChi"));
    }

    /* ---------- khách lẻ ---------- */

    @Test
    void khachLeMuaTaiQuay_tenKhachVangLai_khongSdt_khongEmail_duDaLuuSdtNguoiNhan() {
        hd.setLoaiDon(HoaDon.TAI_QUAY);
        hd.setKhachHang(null);
        hd.setHoTenNguoiNhan("Anh Hùng");
        hd.setSdtNguoiNhan("0912345016");
        Map<String, Object> k = muc("khachHang");
        assertEquals(true, k.get("khachLe"));
        assertEquals("Khách vãng lai", k.get("ten"));
        assertNull(k.get("sdt"), "khách lẻ tại quầy không được hiện số điện thoại");
        assertNull(k.get("email"));

        HoaDonRequest dong = service.getDanhSachCoLoc(null, null, null, null, null, PageRequest.of(0, 5)).getContent().get(0);
        assertEquals("Khách vãng lai", dong.getTenKhachHang());
        assertNull(dong.getSdtKhachHang());
    }

    @Test
    void khachCoHoSoMuaTaiQuay_hienTenVaSdtCuaHoSo() {
        hd.setLoaiDon(HoaDon.TAI_QUAY);
        hd.setKhachHang(khach("Vũ Khánh Linh", "0901000006", "linh@mail.vn"));
        Map<String, Object> k = muc("khachHang");
        assertEquals(false, k.get("khachLe"));
        assertEquals("Vũ Khánh Linh", k.get("ten"));
        assertEquals("0901000006", k.get("sdt"));
        assertEquals("linh@mail.vn", k.get("email"));
    }

    @Test
    void khachCoHoSoNhungChuaCoSdt_taiQuayKhongLaySdtNguoiNhan() {
        hd.setLoaiDon(HoaDon.TAI_QUAY);
        hd.setKhachHang(khach("Vũ Khánh Linh", "  ", null));
        hd.setSdtNguoiNhan("0912345678");
        assertNull(muc("khachHang").get("sdt"));
    }

    @Test
    void donGiaoHangKhongGanHoSo_layTenVaSdtNguoiNhanDeLienHeGiaoHang() {
        hd.setLoaiDon(HoaDon.GIAO_HANG);
        hd.setKhachHang(null);
        hd.setHoTenNguoiNhan("Trần Văn Hùng");
        hd.setSdtNguoiNhan("0901000002");
        Map<String, Object> k = muc("khachHang");
        assertEquals("Trần Văn Hùng", k.get("ten"));
        assertEquals("0901000002", k.get("sdt"));
    }

    @Test
    void donGiaoHangKhachChuaCoSdt_layTamSdtNguoiNhan() {
        hd.setLoaiDon(HoaDon.GIAO_HANG);
        hd.setKhachHang(khach("Phạm Thu Hằng", null, null));
        hd.setSdtNguoiNhan("0901000004");
        assertEquals("0901000004", muc("khachHang").get("sdt"));
    }

    @Test
    void khongCoGiCa_tenKhachVangLai() {
        hd.setLoaiDon(HoaDon.GIAO_HANG);
        hd.setKhachHang(null);
        hd.setHoTenNguoiNhan("   ");
        hd.setSdtNguoiNhan(null);
        Map<String, Object> k = muc("khachHang");
        assertEquals("Khách vãng lai", k.get("ten"));
        assertNull(k.get("sdt"));
    }

    /* ---------- phiếu giảm giá đã dùng ---------- */

    @Test
    void phieuGiamGiaPhanTram_traDuMaLoaiGiaTriVaMucToiDa() {
        hd.setPhieuGiamGia(phieu("VCH7QX2YN", PhieuGiamGia.LOAI_PHAN_TRAM, 10, 100_000L));
        hd.setSoTienGiam(new BigDecimal("50000"));
        Map<String, Object> info = muc("hoaDon");
        assertEquals("VCH7QX2YN", info.get("maPhieuGiamGia"));
        assertEquals("Phiếu VCH7QX2YN", info.get("tenPhieuGiamGia"));
        assertEquals(PhieuGiamGia.LOAI_PHAN_TRAM, info.get("loaiPhieuGiamGia"));
        assertEquals(10L, info.get("giaTriPhieuGiamGia"));
        assertEquals(100_000L, info.get("giamToiDaPhieuGiamGia"));
    }

    @Test
    void phieuGiamGiaSoTien_traLoaiSoTien() {
        hd.setPhieuGiamGia(phieu("VCHTT50KH", PhieuGiamGia.LOAI_SO_TIEN, 50_000, null));
        Map<String, Object> info = muc("hoaDon");
        assertEquals(PhieuGiamGia.LOAI_SO_TIEN, info.get("loaiPhieuGiamGia"));
        assertEquals(50_000L, info.get("giaTriPhieuGiamGia"));
        assertNull(info.get("giamToiDaPhieuGiamGia"));
    }

    @Test
    void khongDungPhieu_cacTruongPhieuDeuNull() {
        hd.setPhieuGiamGia(null);
        Map<String, Object> info = muc("hoaDon");
        for (String khoa : List.of("maPhieuGiamGia", "tenPhieuGiamGia", "loaiPhieuGiamGia", "giaTriPhieuGiamGia", "giamToiDaPhieuGiamGia")) {
            assertTrue(info.containsKey(khoa), khoa);
            assertNull(info.get(khoa), khoa);
        }
    }

    /* ---------- nút đổi trạng thái do backend quyết định ---------- */

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> chuyenDuoc() {
        return (List<Map<String, Object>>) muc("hoaDon").get("chuyenDuoc");
    }

    @Test
    void chiTiet_dangGiaoHang_cungCapNutDaGiaoVaGiaoThatBai_batBuocLyDoDungChoThatBai() {
        hd.setTrangThai(HoaDon.DANG_GIAO);
        List<Map<String, Object>> ds = chuyenDuoc();
        assertEquals(List.of(HoaDon.DA_GIAO, HoaDon.GIAO_THAT_BAI), ds.stream().map(m -> m.get("trangThai")).toList());
        assertEquals(List.of(false, true), ds.stream().map(m -> m.get("batBuocLyDo")).toList());
        assertEquals("Giao hàng thất bại", ds.get(1).get("ten"));
        assertEquals(false, muc("hoaDon").get("laTrangThaiCuoi"));
    }

    @Test
    void chiTiet_daHoanThanhVaDaHuy_khongConNutNaoVaDanhDauTrangThaiCuoi() {
        for (int cuoi : new int[] {HoaDon.HOAN_THANH, HoaDon.DA_HUY}) {
            hd.setTrangThai(cuoi);
            assertTrue(chuyenDuoc().isEmpty(), "trạng thái " + cuoi);
            assertEquals(true, muc("hoaDon").get("laTrangThaiCuoi"), "trạng thái " + cuoi);
        }
    }

    @Test
    void chiTiet_donTaiQuayChoXacNhan_hoanThanhHoacHuy() {
        hd.setLoaiDon(HoaDon.TAI_QUAY);
        hd.setTrangThai(HoaDon.CHO_XAC_NHAN);
        assertEquals(List.of(HoaDon.HOAN_THANH, HoaDon.DA_HUY), chuyenDuoc().stream().map(m -> m.get("trangThai")).toList());
    }

    /* ---------- lịch sử ---------- */

    @Test
    @SuppressWarnings("unchecked")
    void lichSu_giaoHangThatBaiDuocNhanRaLaTrangThai8() {
        LichSuHoaDon ls = new LichSuHoaDon();
        ls.setHoaDon(hd);
        ls.setHanhDong("Giao hàng thất bại");
        ls.setMoTa("Khách không nghe máy");
        ls.setThoiGian(LocalDateTime.of(2026, 10, 15, 9, 0));
        kho.lichSu.add(ls);
        List<Map<String, Object>> lichSu = (List<Map<String, Object>>) service.getChiTietHoaDon(1).get("lichSu");
        assertEquals(1, lichSu.size());
        assertEquals(HoaDon.GIAO_THAT_BAI, lichSu.get(0).get("trangThai"));
        assertFalse(lichSu.get(0).get("hanhDong") == null);
    }
}
