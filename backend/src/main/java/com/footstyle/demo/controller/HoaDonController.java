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
    private final com.footstyle.demo.service.CuaHangService cuaHangService;

    @GetMapping
    public Map<String, Object> getListHoaDon(
            @RequestParam(required = false) String maHoaDon,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
            @RequestParam(required = false) Integer loaiDon,
            @RequestParam(required = false) Integer trangThai,
            @RequestParam(required = false) String kenh,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        // Sắp xếp đơn đặt mới nhất xuống cuối danh sách (đơn cũ lên đầu) để ưu tiên xử lý cho khách đặt trước (FIFO)
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 2000), Sort.by("id").ascending());
        Page<HoaDonRequest> responsePage = hoaDonService.getDanhSachCoLoc(maHoaDon, tuNgay, denNgay, loaiDon, trangThai, kenh, pageable);

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
            @RequestParam(required = false) Integer loaiDon,
            @RequestParam(required = false) String kenh) {
        return hoaDonService.demTheoTrangThai(maHoaDon, tuNgay, denNgay, loaiDon, kenh);
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

    /** Đổi người nhận / SĐT / địa chỉ giao (mỗi đơn 1 lần, trước khi giao cho đơn vị vận chuyển) -> tính lại phí ship. */
    @PutMapping("/{id}/sua-giao-hang")
    public Map<String, Object> suaGiaoHang(@PathVariable Integer id, @RequestBody(required = false) HoaDonSuaService.SuaGiaoHangRequest req) {
        return hoaDonSuaService.suaThongTinGiaoHang(id, req);
    }

    /** Đơn online chuyển khoản: xác nhận đã nhận tiền của khách. */
    @PostMapping("/{id}/xac-nhan-thanh-toan")
    public Map<String, Object> xacNhanThanhToan(@PathVariable Integer id,
                                                @RequestBody(required = false) HoaDonSuaService.XacNhanThanhToanRequest req) {
        return hoaDonSuaService.xacNhanThanhToan(id, req);
    }

    /** Đơn online chuyển khoản chưa thanh toán: Admin đổi trực tiếp sang COD khi khách liên hệ (không cần chờ hết 5 phút). */
    @PutMapping("/{id}/doi-sang-cod")
    public Map<String, Object> doiSangCod(@PathVariable Integer id) {
        return hoaDonSuaService.adminDoiSangCod(id);
    }

    /**
     * 24/11 (Task 3.3): đơn online chuyển khoản chưa thanh toán, còn Chờ xác nhận - gửi thư nhắc thanh toán (mã đơn, số tiền,
     * nội dung chuyển khoản, link trang thanh toán). KHÔNG tạo mã QR mới, KHÔNG gia hạn 5 phút. Trả về chi tiết hóa đơn mới + câu báo.
     */
    @PostMapping("/{id}/gui-mail-nhac-thanh-toan")
    public Map<String, Object> guiMailNhacThanhToan(@PathVariable Integer id) {
        String nguoi = com.footstyle.demo.security.XacThuc.hienTai()
                .filter(com.footstyle.demo.security.NguoiDung::laNhanVien)
                .map(com.footstyle.demo.security.NguoiDung::hoTen)
                .orElse(null);
        Map<String, Object> kq = cuaHangService.guiMailNhacThanhToan(id, nguoi);
        Map<String, Object> res = new HashMap<>(hoaDonService.getChiTietHoaDon(id));
        res.put("thongBao", kq.get("message"));
        return res;
    }

    /** Đơn COD: Xác nhận đã nhận tiền COD từ đơn vị vận chuyển / khách hàng trước khi hoàn thành đơn. */
    @PostMapping("/{id}/xac-nhan-tien-cod")
    public Map<String, Object> xacNhanTienCod(@PathVariable Integer id) {
        return hoaDonSuaService.xacNhanTienCod(id);
    }

    /** Đơn online tạm hết hàng: gửi thư chăm sóc khách (đổi sản phẩm / chờ hàng / hủy), đơn giữ Chờ xác nhận. */
    @PostMapping("/{id}/bao-het-hang")
    public Map<String, Object> baoHetHang(@PathVariable Integer id) {
        return hoaDonSuaService.baoHetHang(id);
    }

    /** Đơn đã hủy có tiền của khách: nhân viên xác nhận đã hoàn trả tiền cho khách. */
    @PostMapping("/{id}/xac-nhan-hoan-tien")
    public Map<String, Object> xacNhanHoanTien(@PathVariable Integer id,
                                               @RequestBody(required = false) HoaDonSuaService.XacNhanThanhToanRequest req) {
        return hoaDonSuaService.xacNhanHoanTien(id, req);
    }
}
