package com.footstyle.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/san-pham-chi-tiet")
public class MockSanPhamChiTietController {

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(List.of(
            Map.of("id", 1, "maSpct", "SP001-DEN-42", "giaBan", 1500000),
            Map.of("id", 2, "maSpct", "SP002-TRANG-40", "giaBan", 2000000)
        ));
    }
}
