package com.footstyle.demo.controller;

import com.footstyle.demo.service.LichLamViecService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Lịch làm việc. /cua-toi: mọi nhân viên (lịch của mình); còn lại: quản lý (PhanQuyen). */
@RestController
@RequestMapping("/api/lich-lam-viec")
@RequiredArgsConstructor
public class LichLamViecController {

    private final LichLamViecService service;

    @GetMapping
    public List<Map<String, Object>> lich(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
                                          @RequestParam(required = false) Integer idNhanVien) {
        return service.lich(tuNgay, denNgay, idNhanVien);
    }

    @GetMapping("/cua-toi")
    public List<Map<String, Object>> lichCuaToi(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
                                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {
        return service.lichCuaToi(tuNgay, denNgay);
    }

    /** Nhân viên đang hoạt động (các hàng của bảng xếp lịch). */
    @GetMapping("/nhan-vien")
    public List<Map<String, Object>> nhanVien() {
        return service.nhanVienDangLam();
    }

    @PostMapping
    public Map<String, Object> xepLich(@RequestBody(required = false) LichLamViecService.XepLichRequest req) {
        return service.xepLich(req);
    }

    @PostMapping("/sao-chep-tuan")
    public Map<String, Object> saoChepTuan(@RequestBody(required = false) Map<String, String> body) {
        return service.saoChepTuan(ngay(body, "tuanNguon"), ngay(body, "tuanDich"));
    }

    private static LocalDate ngay(Map<String, String> body, String khoa) {
        String s = body == null ? null : body.get(khoa);
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.trim());
        } catch (java.time.format.DateTimeParseException e) {
            throw com.footstyle.demo.exception.ApiException.badRequest("Ngày không hợp lệ: " + s + " (định dạng yyyy-MM-dd).");
        }
    }

    @DeleteMapping("/{id}")
    public Map<String, String> xoa(@PathVariable Integer id) {
        service.xoaLich(id);
        return Map.of("message", "Đã xóa lịch.");
    }
}
