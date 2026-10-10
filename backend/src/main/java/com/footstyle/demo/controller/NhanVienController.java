package com.footstyle.demo.controller;

import com.footstyle.demo.dto.NhanVienRequest;
import com.footstyle.demo.dto.NhanVienResponse;
import com.footstyle.demo.service.NhanVienService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/nhan-vien")
@RequiredArgsConstructor
public class NhanVienController {

    private final NhanVienService service;

    @GetMapping
    public List<NhanVienResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public NhanVienResponse getById(@PathVariable Integer id) {
        return service.getById(id);
    }

    @PostMapping
    public ResponseEntity<NhanVienResponse> them(@RequestBody NhanVienRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.them(req));
    }

    @PutMapping("/{id}")
    public NhanVienResponse sua(@PathVariable Integer id, @RequestBody NhanVienRequest req) {
        return service.sua(id, req);
    }

    /** Ẩn / hiện (bật / tắt hoạt động). Không xóa cứng dữ liệu. */
    @PutMapping("/{id}/trang-thai")
    public NhanVienResponse doiTrangThai(@PathVariable Integer id) {
        return service.doiTrangThai(id);
    }
}
