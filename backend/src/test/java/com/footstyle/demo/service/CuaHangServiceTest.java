package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.*;

import com.footstyle.demo.entity.*;
import com.footstyle.demo.repository.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.van-chuyen.ban-do-bat=false") // không gọi API bản đồ trong test: phí theo bảng tỉnh
@Transactional
class CuaHangServiceTest {

    @Autowired
    private CuaHangService cuaHangService;

    @Autowired
    private SanPhamRepository sanPhamRepo;

    @Autowired
    private SanPhamChiTietRepository spctRepo;

    @Autowired
    private DanhMucRepository danhMucRepo;

    @Autowired
    private ThuongHieuRepository thuongHieuRepo;

    @Autowired
    private HoaDonRepository hoaDonRepo;

    /** Biến thể đầu tiên đủ hàng: test chạy trên CSDL thật, sản phẩm đứng đầu danh sách có thể đã bán hết. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> bienTheConHang(int soLuong) {
        for (Map<String, Object> sp : cuaHangService.danhSachSanPham(null, null, null, null, null, null)) {
            for (Map<String, Object> bt : (List<Map<String, Object>>) sp.get("bienThe")) {
                if ((Integer) bt.get("soLuongTon") >= soLuong) return bt;
            }
        }
        return fail("Không còn biến thể nào đủ " + soLuong + " sản phẩm");
    }

    @Test
    void danhSachSanPham_vaDanhMucThuongHieu() {
        Map<String, Object> dmTh = cuaHangService.danhMucVaThuongHieu();
        assertNotNull(dmTh.get("danhMuc"));
        assertNotNull(dmTh.get("thuongHieu"));

        List<Map<String, Object>> sps = cuaHangService.danhSachSanPham(null, null, null, null, null, null);
        assertNotNull(sps);
        assertFalse(sps.isEmpty(), "Phải có sản phẩm bán online");

        Map<String, Object> sp1 = sps.get(0);
        assertNotNull(sp1.get("id"));
        assertNotNull(sp1.get("tenSanPham"));
        assertNotNull(sp1.get("anhChinh"));
        assertNotNull(sp1.get("bienThe"));

        Integer spId = (Integer) sp1.get("id");
        Map<String, Object> chiTiet = cuaHangService.chiTietSanPham(spId);
        assertEquals(spId, chiTiet.get("id"));
    }

    @Test
    void datHangOnline_thanhCong_vaTraCuu() {
        Map<String, Object> bt = bienTheConHang(1);
        Integer idBt = (Integer) bt.get("id");
        int tonCu = (Integer) bt.get("soLuongTon");

        CuaHangService.DatHangOnlineRequest req = new CuaHangService.DatHangOnlineRequest(
                "Khách Hàng Thử Nghiệm",
                "0987654321",
                "khach@gmail.com",
                "123 Cầu Giấy",
                "Hà Nội",
                "Quan Hoa",
                "Giao giờ hành chính",
                null,
                1, // COD
                null,
                List.of(new CuaHangService.DatHangOnlineRequest.ItemRequest(idBt, 1))
        );

        Map<String, Object> donHang = cuaHangService.datHangOnline(req);
        assertNotNull(donHang.get("maHoaDon"));
        assertEquals("Chờ xác nhận", donHang.get("trangThai"));
        assertEquals(1, donHang.get("idPhuongThucThanhToan"));

        String maHd = (String) donHang.get("maHoaDon");

        // Tra cứu lại
        Map<String, Object> traCuu = cuaHangService.traCuuDonHang(maHd, "0987654321");
        assertEquals(maHd, traCuu.get("maHoaDon"));
        assertEquals("Khách Hàng Thử Nghiệm", traCuu.get("hoTenNguoiNhan"));
        assertNotNull(traCuu.get("chiTiet"));

        // Theo yêu cầu của cô: đặt online CHƯA trừ tồn kho (chỉ trừ khi nhân viên xác nhận đơn)
        SanPhamChiTiet btMoi = spctRepo.findById(idBt).orElseThrow();
        assertEquals(tonCu, btMoi.getSoLuongTon());
    }

    @Test
    void datHangOnline_chuyenKhoanVietQR() {
        Integer idBt = (Integer) bienTheConHang(1).get("id");

        CuaHangService.DatHangOnlineRequest req = new CuaHangService.DatHangOnlineRequest(
                "Trần Văn Cầu Lông",
                "0912345678",
                "tran@gmail.com",
                "456 Nguyễn Trãi",
                "Hà Nội",
                "Thanh Xuân",
                "Gọi trước khi giao",
                null,
                2, // Chuyển khoản VietQR
                null,
                List.of(new CuaHangService.DatHangOnlineRequest.ItemRequest(idBt, 1))
        );

        Map<String, Object> donHang = cuaHangService.datHangOnline(req);
        assertNotNull(donHang.get("vietQrUrl"));
        assertTrue(((String) donHang.get("vietQrUrl")).contains("vietqr.io"));
    }

    /** Review 05/11: giỏ hàng hiện số do máy chủ tính trước, phải khớp đúng số khi đặt hàng thật. */
    @Test
    void tinhTien_khopVoiDatHangThat_vaPhieuSaiChiBaoLoi() {
        Integer idBt = (Integer) bienTheConHang(2).get("id");
        List<CuaHangService.DatHangOnlineRequest.ItemRequest> gio = List.of(new CuaHangService.DatHangOnlineRequest.ItemRequest(idBt, 2));

        Map<String, Object> xem = cuaHangService.tinhTien(new CuaHangService.TinhTienRequest(gio, null, null, "Thành phố Hà Nội"));
        long tongGoc = ((Number) xem.get("tongTienGoc")).longValue();
        long giamDot = ((Number) xem.get("tienGiamTheoDot")).longValue();
        long tongTien = ((Number) xem.get("tongTien")).longValue();
        assertEquals(tongGoc - giamDot, tongTien, "tổng theo giá gốc - giảm theo đợt = tiền hàng");
        assertEquals(tongTien + ((Number) xem.get("phiVanChuyen")).longValue(), ((Number) xem.get("thanhTien")).longValue());

        Map<String, Object> don = cuaHangService.datHangOnline(new CuaHangService.DatHangOnlineRequest(
                "Khách Xem Giá", "0900000017", "khach@footstyle.test", "12 Ngõ Thử", "Thành phố Hà Nội", "Phường Hà Đông", null,
                null, 1, null, gio));
        assertEquals(tongTien, ((Number) don.get("tongTien")).longValue());
        assertEquals(((Number) xem.get("phiVanChuyen")).longValue(), ((Number) don.get("phiVanChuyen")).longValue());
        assertEquals(((Number) xem.get("thanhTien")).longValue(), ((Number) don.get("thanhTien")).longValue());

        Map<String, Object> saiMa = cuaHangService.tinhTien(new CuaHangService.TinhTienRequest(gio, "KHONG-CO-MA-NAY", null, null));
        assertEquals("Mã phiếu giảm giá không tồn tại.", saiMa.get("loiPhieu"));
        assertEquals(0L, ((Number) saiMa.get("soTienGiam")).longValue());
        assertNull(saiMa.get("phiVanChuyen"), "chưa chọn tỉnh thì chưa tính được phí giao (phí theo nơi nhận)");
    }

    @Autowired
    private PhieuGiamGiaRepository phieuRepo;

    /** Góp ý 19/11: phiếu bị sửa mức giảm khi khách đang thanh toán -> hỏi lại; "Hủy" giữ nguyên mức đã thấy (chỉ khi có chữ ký máy chủ). */
    @Test
    void phieuBiSuaMucGiam_hoiLai_giuNguyenMucCuChiKhiCoChuKyDung() {
        Integer idBt = (Integer) bienTheConHang(1).get("id");
        List<CuaHangService.DatHangOnlineRequest.ItemRequest> gio = List.of(new CuaHangService.DatHangOnlineRequest.ItemRequest(idBt, 1));
        java.time.LocalDateTime bayGio = java.time.LocalDateTime.now();
        PhieuGiamGia p = new PhieuGiamGia();
        p.setMaPhieu("TSUAMUC19");
        p.setTenPhieu("Phiếu thử sửa mức giảm");
        p.setDoiTuongApDung(PhieuGiamGia.DOI_TUONG_TAT_CA);
        p.setLoaiGiam(PhieuGiamGia.LOAI_SO_TIEN);
        p.setGiaTriGiam(50_000L);
        p.setGiaTriDonHangToiThieu(0L);
        p.setSoLuong(100);
        p.setSoLuongDaDung(0);
        p.setNgayBatDau(bayGio.minusDays(1));
        p.setNgayKetThuc(bayGio.plusDays(1));
        p.setNgayTao(bayGio);
        p.setTrangThai(PhieuGiamGia.HOAT_DONG);
        p = phieuRepo.saveAndFlush(p);

        Map<String, Object> xem = cuaHangService.tinhTien(new CuaHangService.TinhTienRequest(gio, "TSUAMUC19", null, null));
        assertEquals(50_000L, ((Number) xem.get("soTienGiam")).longValue());
        String chot = (String) xem.get("chotPhieu");
        assertNotNull(chot, "báo giá có phiếu thì kèm chữ ký mức giảm");

        p.setGiaTriGiam(30_000L); // quản lý sửa phiếu khi khách đang ở trang thanh toán
        phieuRepo.saveAndFlush(p);

        com.footstyle.demo.exception.ApiException hoi = assertThrows(com.footstyle.demo.exception.ApiException.class,
                () -> cuaHangService.datHangOnline(donThu(gio, 50_000L, null, null)));
        assertEquals("PHIEU_THAY_DOI", hoi.getCode(), "chưa trả lời thì không tự đổi số, hỏi lại khách");
        assertEquals(30_000L, hoi.getChiTiet().get("soTienGiamMoi"));

        com.footstyle.demo.exception.ApiException gia = assertThrows(com.footstyle.demo.exception.ApiException.class,
                () -> cuaHangService.datHangOnline(donThu(gio, 900_000L, true, chot)));
        assertEquals("PHIEU_THAY_DOI", gia.getCode(), "tự gửi mức giảm khác chữ ký thì không được giữ");

        Map<String, Object> giu = cuaHangService.datHangOnline(donThu(gio, 50_000L, true, chot));
        assertEquals(50_000L, ((Number) giu.get("soTienGiam")).longValue(), "Hủy = giữ nguyên mức giảm đã thấy");

        Map<String, Object> moi = cuaHangService.datHangOnline(donThu(gio, 30_000L, null, null));
        assertEquals(30_000L, ((Number) moi.get("soTienGiam")).longValue(), "Áp dụng = mức mới");
    }

    @Test
    void chuKyMucPhieu_hetHanHoacSaiThiKhongHopLe() {
        long luc = 1_000_000L;
        String ky = cuaHangService.kyMucPhieu("ABC", 50_000L, 500_000L, luc);
        assertTrue(cuaHangService.chotPhieuHopLe(ky, "abc", 50_000L, 500_000L, luc + 60));
        assertFalse(cuaHangService.chotPhieuHopLe(ky, "ABC", 50_000L, 600_000L, luc + 60), "giỏ hàng đổi tổng tiền");
        assertFalse(cuaHangService.chotPhieuHopLe(ky, "ABC", 50_000L, 500_000L, luc + CuaHangService.HAN_GIU_MUC_PHIEU_GIAY + 1), "quá hạn");
        assertFalse(cuaHangService.chotPhieuHopLe("1000000.gia", "ABC", 50_000L, 500_000L, luc + 60));
        assertFalse(cuaHangService.chotPhieuHopLe(null, "ABC", 50_000L, 500_000L, luc));
    }

    private static CuaHangService.DatHangOnlineRequest donThu(List<CuaHangService.DatHangOnlineRequest.ItemRequest> gio, Long daThay,
                                                              Boolean giu, String chot) {
        return new CuaHangService.DatHangOnlineRequest("Khách Thử Phiếu", "0900000019", "khach@footstyle.test", "19 Ngõ Thử",
                "Thành phố Hà Nội", "Phường Hà Đông", null, null, 1, "TSUAMUC19", gio, daThay, giu, chot);
    }

    /** Review 05/11 (thầy bấm ra): trang chi tiết không chọn được 3U/4U, chu vi cán vì từng biến thể thiếu mã (id) của chúng. */
    @Test
    @SuppressWarnings("unchecked")
    void sanPham_moiBienTheCoMaTrongLuongVaChuVi() {
        List<Map<String, Object>> sps = cuaHangService.danhSachSanPham(null, null, null, null, null, null);
        assertFalse(sps.isEmpty());
        int soBienTheCoTrongLuong = 0;
        for (Map<String, Object> sp : sps) {
            for (Map<String, Object> bt : (List<Map<String, Object>>) sp.get("bienThe")) {
                assertTrue(bt.containsKey("idTrongLuong") && bt.containsKey("idChuVi"), "biến thể phải gửi đủ mã trọng lượng / chu vi");
                if (bt.get("trongLuong") != null && !"".equals(bt.get("trongLuong"))) {
                    assertNotNull(bt.get("idTrongLuong"), "có trọng lượng thì phải có mã trọng lượng");
                    soBienTheCoTrongLuong++;
                }
                if (bt.get("chuVi") != null && !"".equals(bt.get("chuVi"))) assertNotNull(bt.get("idChuVi"), "có chu vi thì phải có mã chu vi");
            }
        }
        assertTrue(soBienTheCoTrongLuong > 0, "dữ liệu mẫu phải có biến thể chọn được trọng lượng");
    }
}
