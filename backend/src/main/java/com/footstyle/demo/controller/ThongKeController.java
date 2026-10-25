package com.footstyle.demo.controller;

import com.footstyle.demo.service.ThongKeService;
import com.footstyle.demo.service.ThongKeSoSanhService;
import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ThongKeController {

    private final ThongKeService service;
    private final ThongKeSoSanhService soSanh;

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

    public record GuiEmailRequest(String kieu, String ky, String email) {
    }

    /** Gửi báo cáo so sánh (bảng + biểu đồ) tới email. */
    @PostMapping("/so-sanh/gui-email")
    public Map<String, Object> guiEmail(@RequestBody(required = false) GuiEmailRequest req) {
        return soSanh.guiEmail(req == null ? null : req.kieu(), req == null ? null : req.ky(), req == null ? null : req.email());
    }

    /** API trả về dữ liệu liên kết toàn hệ thống từ CSDL cho trang Quản lý Thống kê. */
    @GetMapping("/tong-quan")
    public Map<String, Object> layTongQuan() {
        return service.layTongQuan();
    }
}
