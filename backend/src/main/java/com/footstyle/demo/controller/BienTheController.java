package com.footstyle.demo.controller;

import com.footstyle.demo.dto.BienTheRequest;
import com.footstyle.demo.dto.BienTheResponse;
import com.footstyle.demo.service.BienTheService;
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
@RequestMapping("/api/bien-the-san-pham")
@RequiredArgsConstructor
public class BienTheController {

    private final BienTheService service;

    @GetMapping
    public List<BienTheResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public BienTheResponse getById(@PathVariable Integer id) {
        return service.getById(id);
    }

    @PostMapping
    public ResponseEntity<BienTheResponse> them(@RequestBody BienTheRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.them(req));
    }

    @PutMapping("/{id}")
    public BienTheResponse sua(@PathVariable Integer id, @RequestBody BienTheRequest req) {
        return service.sua(id, req);
    }

    /** Ẩn / hiện biến thể. Không xóa cứng dữ liệu. */
    @PutMapping("/{id}/trang-thai")
    public BienTheResponse doiTrangThai(@PathVariable Integer id) {
        return service.doiTrangThai(id);
    }
}
