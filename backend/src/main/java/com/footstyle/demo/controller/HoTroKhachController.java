package com.footstyle.demo.controller;

import com.footstyle.demo.service.HoTroService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chat hỗ trợ - phía KHÁCH (công khai, không cần đăng nhập). Mỗi cuộc chat có mã truy cập bí mật trả về lúc bắt đầu;
 * các lần sau gửi kèm mã (header X-Ma-Hoi-Thoai cho GET, trường "ma" trong body cho POST).
 */
@RestController
@RequestMapping("/api/ho-tro/khach")
@RequiredArgsConstructor
public class HoTroKhachController {

    private final HoTroService service;

    @PostMapping("/bat-dau")
    public Map<String, Object> batDau(@RequestBody(required = false) HoTroService.BatDauRequest req) {
        return service.batDau(req);
    }

    @GetMapping("/{id}")
    public Map<String, Object> xem(@PathVariable Integer id,
                                   @RequestHeader(value = "X-Ma-Hoi-Thoai", required = false) String ma,
                                   @RequestParam(required = false) Integer sauId) {
        return service.xemKhach(id, ma, sauId);
    }

    @PostMapping("/{id}/tin-nhan")
    public Map<String, Object> gui(@PathVariable Integer id, @RequestBody(required = false) HoTroService.TinKhachRequest req) {
        return service.guiKhach(id, req);
    }

    @PostMapping("/{id}/gap-nhan-vien")
    public Map<String, Object> gapNhanVien(@PathVariable Integer id, @RequestBody(required = false) HoTroService.MaRequest req) {
        return service.gapNhanVien(id, req);
    }

    @PostMapping("/{id}/ket-thuc")
    public Map<String, Object> ketThuc(@PathVariable Integer id, @RequestBody(required = false) HoTroService.MaRequest req) {
        return service.ketThucKhach(id, req);
    }
}
