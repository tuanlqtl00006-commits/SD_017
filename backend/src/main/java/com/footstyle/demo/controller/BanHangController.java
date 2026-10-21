package com.footstyle.demo.controller;

import com.footstyle.demo.service.BanHangService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Bán hàng tại quầy (xem BanHangService). Thêm nhanh khách hàng dùng lại POST /api/khach-hang. */
@RestController
@RequestMapping("/api/ban-hang")
@RequiredArgsConstructor
public class BanHangController {

    private final BanHangService service;

    @GetMapping("/san-pham")
    public List<Map<String, Object>> timSanPham(@RequestParam(required = false) String q,
                                                @RequestParam(required = false) Integer gioiHan) {
        return service.timSanPham(q, gioiHan);
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
}
