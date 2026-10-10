package com.footstyle.demo.controller;

import com.footstyle.demo.dto.KhachHangRequest;
import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.service.KhachHangService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/khach-hang")
@RequiredArgsConstructor
public class KhachHangController {

    private final KhachHangService service;

    @GetMapping
    public List<KhachHang> getAll(@RequestParam(required = false) String search) {
        return service.timKiem(search);
    }

    @GetMapping("/{id}")
    public KhachHang getById(@PathVariable Integer id) {
        return service.layTheoId(id);
    }

    @PostMapping
    public KhachHang create(@RequestBody KhachHangRequest req) {
        return service.them(req);
    }

    @PutMapping("/{id}")
    public KhachHang update(@PathVariable Integer id, @RequestBody KhachHangRequest req) {
        return service.sua(id, req);
    }

    /** Bật / tắt hoạt động khách hàng. */
    @PutMapping("/{id}/trang-thai")
    public KhachHang toggleStatus(@PathVariable Integer id) {
        return service.doiTrangThai(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.xoa(id);
    }
}
