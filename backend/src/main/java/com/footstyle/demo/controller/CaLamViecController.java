package com.footstyle.demo.controller;

import com.footstyle.demo.service.LichLamViecService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Ca làm việc (Ca sáng / chiều / tối...). Xem: mọi nhân viên; thêm / sửa / bật tắt: quản lý (PhanQuyen). */
@RestController
@RequestMapping("/api/ca-lam-viec")
@RequiredArgsConstructor
public class CaLamViecController {

    private final LichLamViecService service;

    @GetMapping
    public List<Map<String, Object>> danhSach() {
        return service.danhSachCa();
    }

    @PostMapping
    public Map<String, Object> them(@RequestBody(required = false) LichLamViecService.CaRequest req) {
        return service.themCa(req);
    }

    @PutMapping("/{id}")
    public Map<String, Object> sua(@PathVariable Integer id, @RequestBody(required = false) LichLamViecService.CaRequest req) {
        return service.suaCa(id, req);
    }

    @PutMapping("/{id}/trang-thai")
    public Map<String, Object> doiTrangThai(@PathVariable Integer id) {
        return service.doiTrangThaiCa(id);
    }
}
