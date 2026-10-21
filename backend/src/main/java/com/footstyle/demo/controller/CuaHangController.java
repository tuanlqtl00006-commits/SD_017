package com.footstyle.demo.controller;

import com.footstyle.demo.service.CuaHangService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
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
}
