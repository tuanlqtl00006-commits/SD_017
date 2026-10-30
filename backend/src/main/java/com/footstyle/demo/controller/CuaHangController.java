package com.footstyle.demo.controller;

import com.footstyle.demo.service.CuaHangService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cua-hang")
@RequiredArgsConstructor
public class CuaHangController {

    private final CuaHangService service;

    @GetMapping("/san-pham")
    public List<Map<String, Object>> danhSachSanPham(
            @RequestParam(required = false) Integer idDanhMuc,
            @RequestParam(required = false) Integer idThuongHieu,
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(required = false) Long giaTu,
            @RequestParam(required = false) Long giaDen,
            @RequestParam(required = false) String sapXep
    ) {
        return service.danhSachSanPham(idDanhMuc, idThuongHieu, tuKhoa, giaTu, giaDen, sapXep);
    }

    @GetMapping("/san-pham/{id}")
    public Map<String, Object> chiTietSanPham(@PathVariable Integer id) {
        return service.chiTietSanPham(id);
    }

    @GetMapping("/danh-muc-va-thuong-hieu")
    public Map<String, Object> danhMucVaThuongHieu() {
        return service.danhMucVaThuongHieu();
    }

    @GetMapping("/khuyen-mai")
    public List<Map<String, Object>> sanPhamKhuyenMai() {
        return service.sanPhamKhuyenMai();
    }

    @GetMapping("/phieu-giam-gia")
    public List<Map<String, Object>> phieuGiamGia(@RequestParam(required = false) Long tongTien) {
        return service.phieuGiamGiaCongKhai(tongTien);
    }

    @PostMapping("/dat-hang")
    public Map<String, Object> datHangOnline(@RequestBody CuaHangService.DatHangOnlineRequest req) {
        return service.datHangOnline(req);
    }

    @GetMapping("/tra-cuu")
    public Map<String, Object> traCuuDonHang(
            @RequestParam String maHoaDon,
            @RequestParam(required = false) String sdt
    ) {
        return service.traCuuDonHang(maHoaDon, sdt);
    }

    // Tài khoản nhận tiền hiển thị trong mã VietQR (đổi bằng biến môi trường / application.properties).
    // Mặc định là tài khoản MẪU để thử giao diện, KHÔNG nhận tiền thật.
    @Value("${app.thanh-toan.ngan-hang:MB}")
    private String nganHang;
    @Value("${app.thanh-toan.so-tai-khoan:0123456789}")
    private String soTaiKhoan;
    @Value("${app.thanh-toan.ten-chu-tai-khoan:FOOTSTYLE DEMO}")
    private String tenChuTaiKhoan;

    @GetMapping("/thong-tin-thanh-toan")
    public Map<String, Object> thongTinThanhToan() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nganHang", nganHang);
        m.put("soTaiKhoan", soTaiKhoan);
        m.put("tenChuTaiKhoan", tenChuTaiKhoan);
        return m;
    }
}
