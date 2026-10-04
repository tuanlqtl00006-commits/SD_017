package com.footstyle.demo.controller;

import com.footstyle.demo.dto.SanPhamRequest;
import com.footstyle.demo.dto.SanPhamResponse;
import com.footstyle.demo.service.SanPhamService;
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
@RequestMapping("/api/san-pham")
@RequiredArgsConstructor
public class SanPhamController {

    private final SanPhamService service;

    @GetMapping
    public List<SanPhamResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public SanPhamResponse getById(@PathVariable Integer id) {
        return service.getById(id);
    }

    @PostMapping
    public ResponseEntity<SanPhamResponse> them(@RequestBody SanPhamRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.them(req));
    }

    @PutMapping("/{id}")
    public SanPhamResponse sua(@PathVariable Integer id, @RequestBody SanPhamRequest req) {
        return service.sua(id, req);
    }
}
