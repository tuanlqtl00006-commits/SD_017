package com.footstyle.demo.controller;

import com.footstyle.demo.dto.HoaDonRequest;
import com.footstyle.demo.dto.HoaDonTrangThaiRequest;
import com.footstyle.demo.service.HoaDonService;
import com.footstyle.demo.service.HoaDonSuaService;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hoa-don")
@RequiredArgsConstructor
public class HoaDonController {

    private final HoaDonService hoaDonService;
    private final HoaDonSuaService hoaDonSuaService;

    @GetMapping
    public Map<String, Object> getListHoaDon(
            @RequestParam(required = false) String maHoaDon,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
            @RequestParam(required = false) Integer loaiDon,
            @RequestParam(required = false) Integer trangThai,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 2000), Sort.by("id").descending());
        Page<HoaDonRequest> responsePage = hoaDonService.getDanhSachCoLoc(maHoaDon, tuNgay, denNgay, loaiDon, trangThai, pageable);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("content", responsePage.getContent());
        responseMap.put("totalPages", responsePage.getTotalPages());
        responseMap.put("number", responsePage.getNumber());
        responseMap.put("totalElements", responsePage.getTotalElements());
        return responseMap;
    }

    /** Số hóa đơn theo từng trạng thái (các tab của trang danh sách), áp cùng bộ lọc mã / ngày / loại đơn. */
    @GetMapping("/dem-trang-thai")
    public Map<String, Long> demTheoTrangThai(
            @RequestParam(required = false) String maHoaDon,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
            @RequestParam(required = false) Integer loaiDon) {
        return hoaDonService.demTheoTrangThai(maHoaDon, tuNgay, denNgay, loaiDon);
    }

    /** Tra cứu nhanh hóa đơn theo mã (phục vụ quét mã QR / mã vạch). */
    @GetMapping("/tra-cuu")
    public Map<String, Object> traCuuHoaDon(@RequestParam String ma) {
        return hoaDonService.traCuuTheoMa(ma);
    }

    /** Toàn bộ dữ liệu cho trang chi tiết: thông tin, khách, giao hàng, sản phẩm, thanh toán, lịch sử. */
    @GetMapping("/{id}")
    public Map<String, Object> getChiTietHoaDon(@PathVariable Integer id) {
        return hoaDonService.getChiTietHoaDon(id);
    }

    /** Chuyển sang trạng thái tiếp theo hoặc hủy đơn; trả về chi tiết hóa đơn sau khi đổi. */
    @PutMapping("/{id}/trang-thai")
    public Map<String, Object> doiTrangThai(@PathVariable Integer id, @RequestBody HoaDonTrangThaiRequest req) {
        return hoaDonService.doiTrangThai(id, req);
    }

    /** Sửa đơn online MỘT lần khi còn Chờ xác nhận (người nhận, địa chỉ, sản phẩm) -> tính lại tiền, hoàn phí / phụ phí. */
    @PutMapping("/{id}/sua-don")
    public Map<String, Object> suaDon(@PathVariable Integer id, @RequestBody(required = false) HoaDonSuaService.SuaDonRequest req) {
        return hoaDonSuaService.suaDon(id, req);
    }

    /** Đơn online chuyển khoản: xác nhận đã nhận tiền của khách. */
    @PostMapping("/{id}/xac-nhan-thanh-toan")
    public Map<String, Object> xacNhanThanhToan(@PathVariable Integer id,
                                                @RequestBody(required = false) HoaDonSuaService.XacNhanThanhToanRequest req) {
        return hoaDonSuaService.xacNhanThanhToan(id, req);
    }
}
