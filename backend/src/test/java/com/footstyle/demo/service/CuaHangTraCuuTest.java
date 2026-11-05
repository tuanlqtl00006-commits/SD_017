package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.KhoGia;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Khách chưa đăng nhập chỉ xem được đơn khi nhập đúng mã đơn + SĐT đặt hàng (cô review buổi 7, 9:15–9:31). */
class CuaHangTraCuuTest {

    private CuaHangService service;
    private HoaDon hoaDonChoQuay;

    @BeforeEach
    void setUp() {
        hoaDonChoQuay = new HoaDon();
        hoaDonChoQuay.setId(968);
        hoaDonChoQuay.setMaHoaDon("HD968");
        hoaDonChoQuay.setLoaiDon(HoaDon.TAI_QUAY);
        hoaDonChoQuay.setTrangThai(HoaDon.CHO_XAC_NHAN);
        hoaDonChoQuay.setSdtNguoiNhan("0379992712");
        KhachHang kh = new KhachHang();
        kh.setId(5);
        kh.setSdt("0911222333");
        HoaDon hd = new HoaDon();
        hd.setId(967);
        hd.setMaHoaDon("HD967");
        hd.setKhachHang(kh);
        hd.setHoTenNguoiNhan("tuan134");
        hd.setSdtNguoiNhan("0379992712");
        hd.setDiaChiGiaoHang("so,ngo, Phường Bình Lộc, Thành phố Đồng Nai");
        hd.setTrangThai(1);
        hd.setNgayTao(LocalDateTime.of(2026, 11, 4, 7, 36));
        hd.setTongTien(BigDecimal.valueOf(468_000));
        hd.setThanhTien(BigDecimal.valueOf(468_000));

        service = new CuaHangService(
                KhoGia.khongDung(SanPhamRepository.class), KhoGia.khongDung(SanPhamChiTietRepository.class),
                KhoGia.khongDung(HinhAnhSanPhamRepository.class), KhoGia.khongDung(DanhMucRepository.class),
                KhoGia.khongDung(ThuongHieuRepository.class), KhoGia.khongDung(XuatXuRepository.class),
                KhoGia.khongDung(ChatLieuRepository.class), KhoGia.khongDung(DoCungRepository.class),
                KhoGia.khongDung(DiemCanBangRepository.class), KhoGia.khongDung(MauSacRepository.class),
                KhoGia.khongDung(TrongLuongRepository.class), KhoGia.khongDung(ChuViRepository.class),
                KhoGia.tao(HoaDonRepository.class, (t, a) -> switch (t) {
                    case "findFirstByMaHoaDonIgnoreCase" -> Optional.ofNullable("HD967".equalsIgnoreCase((String) a[0]) ? hd : null);
                    // tra cứu theo SĐT: mới nhất trước; số 0379992712 có thêm một hóa đơn chờ tại quầy chưa thanh toán (phải bị bỏ)
                    case "timTheoSdt" -> "0379992712".equals(a[0]) ? List.of(hoaDonChoQuay, hd)
                            : "0911222333".equals(a[0]) ? List.of(hd) : List.of();
                    default -> KhoGia.KHONG_CO;
                }),
                KhoGia.tao(HoaDonChiTietRepository.class, (t, a) -> "findByHoaDon_IdOrderByIdAsc".equals(t) ? List.of() : KhoGia.KHONG_CO),
                KhoGia.khongDung(LichSuHoaDonRepository.class),
                KhoGia.tao(LichSuThanhToanRepository.class, (t, a) -> "findByHoaDon_IdOrderByThoiGianAscIdAsc".equals(t) ? List.of() : KhoGia.KHONG_CO),
                KhoGia.khongDung(PhieuGiamGiaRepository.class), KhoGia.khongDung(PhieuGiamGiaKhachHangRepository.class),
                KhoGia.khongDung(KhachHangRepository.class), null);
    }

    private static ApiException loi(Runnable r) {
        return assertThrows(ApiException.class, r::run);
    }

    @Test
    void chiCoMaDon_khongXemDuocThongTinNguoiNhan() {
        assertTrue(loi(() -> service.traCuuDonHang("HD967", null)).getMessage().contains("số điện thoại"));
        assertTrue(loi(() -> service.traCuuDonHang("HD967", "  ")).getMessage().contains("số điện thoại"));
    }

    @Test
    void saiSdt_baoKhongTimThay_giongNhuDonKhongTonTai() {
        String saiSdt = loi(() -> service.traCuuDonHang("HD967", "0999999999")).getMessage();
        String khongCo = loi(() -> service.traCuuDonHang("HD999", "0379992712")).getMessage();
        assertTrue(saiSdt.startsWith("Không tìm thấy đơn hàng") && khongCo.startsWith("Không tìm thấy đơn hàng"),
                "không để lộ là đơn có tồn tại");
    }

    @Test
    void dungMaVaSdtNguoiNhanHoacSdtKhach_thiXemDuoc() {
        Map<String, Object> d = service.traCuuDonHang("hd967", "0379 992 712");
        assertEquals("HD967", d.get("maHoaDon"));
        assertEquals("tuan134", d.get("hoTenNguoiNhan"));
        assertEquals("HD967", service.traCuuDonHang("HD967", "0911222333").get("maHoaDon"));
    }

    /** Tra cứu theo số điện thoại: chỉ danh sách gọn (không họ tên / địa chỉ), bỏ hóa đơn chờ tại quầy, SĐT sai báo rõ. */
    @Test
    @SuppressWarnings("unchecked")
    void traCuuTheoSdt_chiTraDanhSachGon_boHoaDonChoTaiQuay() {
        Map<String, Object> kq = service.traCuuTheoSdt("0379 992 712");
        assertEquals("0379992712", kq.get("sdt"));
        List<Map<String, Object>> don = (List<Map<String, Object>>) kq.get("don");
        assertEquals(1, don.size(), "bỏ HD968 là hóa đơn chờ tại quầy chưa thanh toán");
        Map<String, Object> d = don.get(0);
        assertEquals("HD967", d.get("maHoaDon"));
        assertEquals("Chờ xác nhận", d.get("tenTrangThai"));
        assertEquals("Trực tuyến", d.get("loaiDon"));
        assertEquals(BigDecimal.valueOf(468_000), d.get("thanhTien"));
        assertTrue(!d.containsKey("hoTenNguoiNhan") && !d.containsKey("diaChiGiaoHang"), "danh sách không lộ họ tên, địa chỉ");
        // số khách hàng (không phải số người nhận) cũng ra đơn của khách đó
        assertEquals("HD967", ((List<Map<String, Object>>) service.traCuuTheoSdt("0911222333").get("don")).get(0).get("maHoaDon"));
    }

    @Test
    void traCuuTheoSdt_sdtSaiDinhDang_hoacKhongCoDon() {
        for (String sai : new String[] {null, "", "abc", "12345", "1379992712", "03799927123"}) {
            ApiException e = loi(() -> service.traCuuTheoSdt(sai));
            assertEquals(400, e.getStatus().value(), String.valueOf(sai));
            assertTrue(e.getMessage().contains("10 chữ số"), e.getMessage());
        }
        ApiException khong = loi(() -> service.traCuuTheoSdt("0999999999"));
        assertEquals(404, khong.getStatus().value());
        assertTrue(khong.getMessage().contains("Không tìm thấy đơn hàng nào"));
    }

    @Test
    void xemDonCuaKhachDangNhap_chiDonCuaMinh() {
        assertEquals("HD967", service.traCuuDonHangCuaKhach("HD967", 5).get("maHoaDon"));
        loi(() -> service.traCuuDonHangCuaKhach("HD967", 6));
    }

    /** Cô review 04/11: màn đơn của khách y hệt màn admin, nhưng không lộ phần nội bộ của cửa hàng. */
    @Test
    void chiTietChoKhach_cungDangAdmin_boPhanNoiBo() {
        Map<String, Object> info = new java.util.LinkedHashMap<>();
        info.put("maHoaDon", "HD971");
        info.put("trangThai", 0);
        info.put("chuyenDuoc", List.of(Map.of("trangThai", 2)));
        info.put("duocChinhSua", true);
        info.put("daChinhSua", false);
        info.put("tenNhanVien", "Nguyễn Hoàng Long");
        info.put("maNhanVien", "NV01");
        Map<String, Object> chiTiet = new java.util.LinkedHashMap<>();
        chiTiet.put("hoaDon", info);
        chiTiet.put("sanPham", List.of(Map.of("maSpct", "SP53-TRG")));
        chiTiet.put("duocXacNhanThanhToan", true);
        chiTiet.put("lichSu", List.of(
                lichSu(1, "Nguyễn Hoàng Long", "Đặt hàng online"),
                lichSu(null, "Nguyễn Hoàng Long", "Sửa đơn: đổi địa chỉ (ghi chú nội bộ)"),
                lichSu(0, "lê quốc tuấn", "Khách đổi ý")));

        Map<String, Object> kq = CuaHangService.banChoKhach(chiTiet);

        @SuppressWarnings("unchecked") Map<String, Object> hd = (Map<String, Object>) kq.get("hoaDon");
        assertEquals("HD971", hd.get("maHoaDon"));
        for (String k : List.of("chuyenDuoc", "duocChinhSua", "daChinhSua", "tenNhanVien", "maNhanVien")) {
            assertTrue(!hd.containsKey(k), "không trả " + k);
        }
        assertTrue(!kq.containsKey("duocXacNhanThanhToan"));
        assertEquals(chiTiet.get("sanPham"), kq.get("sanPham"));
        @SuppressWarnings("unchecked") List<Map<String, Object>> ls = (List<Map<String, Object>>) kq.get("lichSu");
        assertEquals(2, ls.size(), "bỏ thao tác nội bộ không phải bước trạng thái");
        assertTrue(ls.stream().noneMatch(m -> m.containsKey("nguoiThaoTac")), "không lộ người thao tác");
        assertTrue(!ls.get(0).containsKey("moTa"), "ghi chú thường là nội bộ");
        assertEquals("Khách đổi ý", ls.get(1).get("moTa"), "lý do hủy thì khách được xem");
        // Bản gốc của admin không bị đổi
        assertTrue(info.containsKey("chuyenDuoc"));
    }

    private static Map<String, Object> lichSu(Integer trangThai, String nguoi, String moTa) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("trangThai", trangThai);
        m.put("nguoiThaoTac", nguoi);
        m.put("moTa", moTa);
        m.put("thoiGian", LocalDateTime.of(2026, 11, 4, 20, 52));
        return m;
    }
}
