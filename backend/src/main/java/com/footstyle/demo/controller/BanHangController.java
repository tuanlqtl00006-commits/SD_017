package com.footstyle.demo.controller;

import com.footstyle.demo.service.BanHangService;
import com.footstyle.demo.service.HoaDonChoService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Bán hàng tại quầy (xem BanHangService, HoaDonChoService). Thêm nhanh khách hàng dùng lại POST /api/khach-hang. */
@RestController
@RequestMapping("/api/ban-hang")
@RequiredArgsConstructor
public class BanHangController {

    private final BanHangService service;
    private final HoaDonChoService hoaDonCho;

    @GetMapping("/san-pham")
    public List<Map<String, Object>> timSanPham(@RequestParam(required = false) String q,
                                                @RequestParam(required = false) Integer gioiHan) {
        return service.timSanPham(q, gioiHan);
    }

    /** Bảng chọn sản phẩm: lọc như trang sản phẩm + khoảng giá, mới nhất trước, phân trang. */
    @GetMapping("/san-pham/loc")
    public Map<String, Object> locSanPham(BanHangService.LocSanPham loc) {
        return service.locSanPham(loc);
    }

    /** Quét mã QR / mã vạch của biến thể. */
    @GetMapping("/san-pham/ma/{ma}")
    public Map<String, Object> timTheoMa(@PathVariable String ma) {
        return service.timTheoMa(ma);
    }

    @GetMapping("/khach-hang")
    public List<Map<String, Object>> timKhach(@RequestParam(required = false) String q) {
        return service.timKhach(q);
    }

    @GetMapping("/phieu-giam-gia")
    public List<Map<String, Object>> phieu(@RequestParam(required = false) Integer idKhachHang,
                                           @RequestParam(required = false) Long tongTien) {
        return service.phieuChoDon(idKhachHang, tongTien);
    }

    @PostMapping("/thanh-toan")
    public Map<String, Object> thanhToan(@RequestBody(required = false) BanHangService.ThanhToanRequest req) {
        return service.thanhToan(req);
    }

    /* ----- Hóa đơn chờ (lưu ở CSDL, trừ kho ngay khi thêm sản phẩm) ----- */

    @GetMapping("/hoa-don-cho")
    public List<Map<String, Object>> dsHoaDonCho() {
        return hoaDonCho.danhSach();
    }

    @PostMapping("/hoa-don-cho")
    public Map<String, Object> taoHoaDonCho() {
        return hoaDonCho.tao();
    }

    @DeleteMapping("/hoa-don-cho/{id}")
    public void huyHoaDonCho(@PathVariable Integer id) {
        hoaDonCho.huy(id);
    }

    @PutMapping("/hoa-don-cho/{id}/khach-hang")
    public Map<String, Object> doiKhach(@PathVariable Integer id, @RequestBody(required = false) HoaDonChoService.DoiKhachRequest req) {
        return hoaDonCho.doiKhach(id, req);
    }

    @PostMapping("/hoa-don-cho/{id}/san-pham")
    public Map<String, Object> themSanPham(@PathVariable Integer id, @RequestBody(required = false) HoaDonChoService.ThemSanPhamRequest req) {
        return hoaDonCho.themSanPham(id, req);
    }

    @PutMapping("/hoa-don-cho/{id}/chi-tiet/{idChiTiet}")
    public Map<String, Object> doiSoLuong(@PathVariable Integer id, @PathVariable Integer idChiTiet,
                                          @RequestBody(required = false) HoaDonChoService.SoLuongRequest req) {
        return hoaDonCho.doiSoLuong(id, idChiTiet, req);
    }

    @DeleteMapping("/hoa-don-cho/{id}/chi-tiet/{idChiTiet}")
    public Map<String, Object> xoaDong(@PathVariable Integer id, @PathVariable Integer idChiTiet) {
        return hoaDonCho.xoaDong(id, idChiTiet);
    }

    @PostMapping("/hoa-don-cho/{id}/cap-nhat-gia")
    public Map<String, Object> capNhatGia(@PathVariable Integer id) {
        return hoaDonCho.capNhatGia(id);
    }

    @PostMapping("/hoa-don-cho/{id}/thanh-toan")
    public Map<String, Object> thanhToanHoaDonCho(@PathVariable Integer id, @RequestBody(required = false) BanHangService.ThanhToanChoRequest req) {
        return service.thanhToanHoaDonCho(id, req);
    }
}
