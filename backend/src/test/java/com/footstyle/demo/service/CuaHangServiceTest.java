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

        // Kiểm tra tồn kho đã giảm 1
        SanPhamChiTiet btMoi = spctRepo.findById(idBt).orElseThrow();
        assertEquals(tonCu - 1, btMoi.getSoLuongTon());
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
}
