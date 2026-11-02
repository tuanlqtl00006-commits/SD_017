package com.footstyle.demo.controller;

import com.footstyle.demo.service.ThongKeService;
import com.footstyle.demo.service.ThongKeSoSanhService;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Thống kê giai đoạn 3 (chỉ quản lý - xem PhanQuyen). */
@RestController
@RequestMapping("/api/thong-ke")
public class ThongKeController {

    private final ThongKeService service;
    private final ThongKeSoSanhService soSanh;

    public ThongKeController(ThongKeService service, ThongKeSoSanhService soSanh) {
        this.service = service;
        this.soSanh = soSanh;
    }

    @GetMapping
    public Map<String, Object> tongHop(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
                                       @RequestParam(required = false) Integer nguong) {
        return service.tongHop(tuNgay, denNgay, nguong);
    }

    /** So sánh theo tháng / quý / năm: kieu = THANG | QUY | NAM, ky = 2026-10 | 2026-Q4 | 2026 (bỏ trống = kỳ hiện tại). */
    @GetMapping("/so-sanh")
    public Map<String, Object> soSanh(@RequestParam(required = false) String kieu, @RequestParam(required = false) String ky) {
        return soSanh.soSanh(kieu, ky);
    }

    public record GuiEmailRequest(String kieu, String ky, String email, String tieuDe, String ghiChu, java.util.List<String> dsKy) {
    }

    public record LichTuDongRequest(String email, Boolean hangNgay, Boolean hangThang, Boolean hangNam) {
    }

    /** Gửi báo cáo so sánh (bảng + biểu đồ) tới email. */
    @PostMapping("/so-sanh/gui-email")
    public Map<String, Object> guiEmail(@RequestBody(required = false) GuiEmailRequest req) {
        if (req == null) return soSanh.guiEmail(null, null, null);
        return soSanh.guiEmail(req.kieu(), req.ky(), req.email(), req.tieuDe(), req.ghiChu(), req.dsKy());
    }

    /** Lấy cấu hình lịch gửi báo cáo tự động. */
    @GetMapping("/so-sanh/lich-tu-dong")
    public Map<String, Object> layLichTuDong() {
        return soSanh.layLichTuDong();
    }

    /** Lưu cấu hình lịch gửi báo cáo tự động. */
    @PostMapping("/so-sanh/lich-tu-dong")
    public Map<String, Object> luuLichTuDong(@RequestBody(required = false) LichTuDongRequest req) {
        if (req == null) return soSanh.layLichTuDong();
        return soSanh.luuLichTuDong(req.email(), req.hangNgay(), req.hangThang(), req.hangNam());
    }

    /** API trả về dữ liệu liên kết toàn hệ thống từ CSDL cho trang Quản lý Thống kê. */
    @GetMapping("/tong-quan")
    public Map<String, Object> layTongQuan() {
        return service.layTongQuan();
    }
}
