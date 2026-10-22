package com.footstyle.demo.controller;

import com.footstyle.demo.service.ThongKeService;
import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Thống kê giai đoạn 3 (chỉ quản lý - xem PhanQuyen). */
@RestController
@RequestMapping("/api/thong-ke")
@RequiredArgsConstructor
public class ThongKeController {

    private final ThongKeService service;

    @GetMapping
    public Map<String, Object> tongHop(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
                                       @RequestParam(required = false) Integer nguong) {
        return service.tongHop(tuNgay, denNgay, nguong);
    }

    /** API trả về dữ liệu liên kết toàn hệ thống từ CSDL cho trang Quản lý Thống kê. */
    @GetMapping("/tong-quan")
    public Map<String, Object> layTongQuan() {
        return service.layTongQuan();
    }
}
