package com.footstyle.demo.controller;

import java.util.Map;

import java.util.Optional;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.footstyle.demo.entity.DotGiamGia;
import com.footstyle.demo.repository.DotGiamGiaRepository;

@RestController
@RequestMapping("/api/dot-giam-gia")
public class DotGiamGiaController {

    @Autowired
    private DotGiamGiaRepository dotGiamGiaRepository;

    @GetMapping
    public List<DotGiamGia> getAll() {
        return dotGiamGiaRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DotGiamGia> getById(@PathVariable Long id) {
        Optional<DotGiamGia> dgg = dotGiamGiaRepository.findById(id);
        return dgg.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private int calculateStatus(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (start != null && end != null) {
            if (now.isBefore(start)) return 2;
            if (now.isAfter(end)) return 0;
            return 1;
        }
        return 1;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody DotGiamGia dgg) {
        // Validate
        if (dgg.getTenDot() == null || dgg.getTenDot().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tên đợt giảm giá không được để trống"));
        }
        if (dgg.getPhanTramGiamDot() == null || dgg.getPhanTramGiamDot() < 1 || dgg.getPhanTramGiamDot() > 100) {
            return ResponseEntity.badRequest().body(Map.of("message", "Phần trăm giảm phải nằm trong khoảng từ 1 đến 100"));
        }
        if (dgg.getNgayBatDau() == null || dgg.getNgayKetThuc() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Ngày bắt đầu và kết thúc không được để trống"));
        }
        if (dgg.getNgayKetThuc().isBefore(dgg.getNgayBatDau())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Ngày kết thúc phải sau ngày bắt đầu"));
        }

        if (dgg.getMaDot() == null || dgg.getMaDot().trim().isEmpty()) {
            dgg.setMaDot("DGG" + System.currentTimeMillis());
        } else if (dotGiamGiaRepository.existsByMaDot(dgg.getMaDot())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mã đợt giảm giá đã tồn tại trong hệ thống"));
        }

        dgg.setTrangThai(calculateStatus(dgg.getNgayBatDau(), dgg.getNgayKetThuc()));
        return ResponseEntity.ok(dotGiamGiaRepository.save(dgg));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody DotGiamGia dggDetails) {
        // Validate
        if (dggDetails.getTenDot() == null || dggDetails.getTenDot().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tên đợt giảm giá không được để trống"));
        }
        if (dggDetails.getPhanTramGiamDot() == null || dggDetails.getPhanTramGiamDot() < 1 || dggDetails.getPhanTramGiamDot() > 100) {
            return ResponseEntity.badRequest().body(Map.of("message", "Phần trăm giảm phải nằm trong khoảng từ 1 đến 100"));
        }
        if (dggDetails.getNgayBatDau() == null || dggDetails.getNgayKetThuc() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Ngày bắt đầu và kết thúc không được để trống"));
        }
        if (dggDetails.getNgayKetThuc().isBefore(dggDetails.getNgayBatDau())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Ngày kết thúc phải sau ngày bắt đầu"));
        }

        Optional<DotGiamGia> optional = dotGiamGiaRepository.findById(id);
        if (optional.isPresent()) {
            DotGiamGia existing = optional.get();
            existing.setTenDot(dggDetails.getTenDot());
            existing.setPhanTramGiamDot(dggDetails.getPhanTramGiamDot());
            existing.setNgayBatDau(dggDetails.getNgayBatDau());
            existing.setNgayKetThuc(dggDetails.getNgayKetThuc());
            existing.setTrangThai(calculateStatus(dggDetails.getNgayBatDau(), dggDetails.getNgayKetThuc()));
            return ResponseEntity.ok(dotGiamGiaRepository.save(existing));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (dotGiamGiaRepository.existsById(id)) {
            dotGiamGiaRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}

