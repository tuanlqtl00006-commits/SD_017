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

@SpringBootTest
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
        List<Map<String, Object>> sps = cuaHangService.danhSachSanPham(null, null, null, null, null, null);
        assertFalse(sps.isEmpty());

        Map<String, Object> sp = sps.get(0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bts = (List<Map<String, Object>>) sp.get("bienThe");
        assertFalse(bts.isEmpty());

        Integer idBt = (Integer) bts.get(0).get("id");
        int tonCu = (Integer) bts.get(0).get("soLuongTon");

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
        List<Map<String, Object>> sps = cuaHangService.danhSachSanPham(null, null, null, null, null, null);
        assertFalse(sps.isEmpty());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bts = (List<Map<String, Object>>) sps.get(0).get("bienThe");
        Integer idBt = (Integer) bts.get(0).get("id");

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
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bts = (List<Map<String, Object>>) cuaHangService.danhSachSanPham(null, null, null, null, null, null).get(0).get("bienThe");
        Integer idBt = (Integer) bts.get(0).get("id");
        List<CuaHangService.DatHangOnlineRequest.ItemRequest> gio = List.of(new CuaHangService.DatHangOnlineRequest.ItemRequest(idBt, 2));

        Map<String, Object> xem = cuaHangService.tinhTien(new CuaHangService.TinhTienRequest(gio, null, null, "Thành phố Hà Nội"));
        long tongGoc = ((Number) xem.get("tongTienGoc")).longValue();
        long giamDot = ((Number) xem.get("tienGiamTheoDot")).longValue();
        long tongTien = ((Number) xem.get("tongTien")).longValue();
        assertEquals(tongGoc - giamDot, tongTien, "tổng theo giá gốc - giảm theo đợt = tiền hàng");
        assertEquals(tongTien + ((Number) xem.get("phiVanChuyen")).longValue(), ((Number) xem.get("thanhTien")).longValue());

        Map<String, Object> don = cuaHangService.datHangOnline(new CuaHangService.DatHangOnlineRequest(
                "Khách Xem Giá", "0900000017", null, "12 Ngõ Thử", "Thành phố Hà Nội", "Phường Hà Đông", null,
                null, 1, null, gio));
        assertEquals(tongTien, ((Number) don.get("tongTien")).longValue());
        assertEquals(((Number) xem.get("phiVanChuyen")).longValue(), ((Number) don.get("phiVanChuyen")).longValue());
        assertEquals(((Number) xem.get("thanhTien")).longValue(), ((Number) don.get("thanhTien")).longValue());

        Map<String, Object> saiMa = cuaHangService.tinhTien(new CuaHangService.TinhTienRequest(gio, "KHONG-CO-MA-NAY", null, null));
        assertEquals("Mã phiếu giảm giá không tồn tại.", saiMa.get("loiPhieu"));
        assertEquals(0L, ((Number) saiMa.get("soTienGiam")).longValue());
        assertEquals(null, saiMa.get("phiVanChuyen"), "chưa chọn tỉnh thì chưa tính phí giao");
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
