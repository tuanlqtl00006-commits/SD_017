package com.footstyle.demo.controller;

import com.footstyle.demo.entity.DotGiamGia;
import com.footstyle.demo.repository.DotGiamGiaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

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

    @PostMapping
    public DotGiamGia create(@RequestBody DotGiamGia dgg) {
        if (dgg.getMaDot() == null || dgg.getMaDot().isEmpty()) {
            dgg.setMaDot("DGG" + System.currentTimeMillis());
        }
        return dotGiamGiaRepository.save(dgg);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DotGiamGia> update(@PathVariable Long id, @RequestBody DotGiamGia dggDetails) {
        Optional<DotGiamGia> optional = dotGiamGiaRepository.findById(id);
        if (optional.isPresent()) {
            DotGiamGia existing = optional.get();
            existing.setTenDot(dggDetails.getTenDot());
            existing.setPhanTramGiamDot(dggDetails.getPhanTramGiamDot());
            existing.setNgayBatDau(dggDetails.getNgayBatDau());
            existing.setNgayKetThuc(dggDetails.getNgayKetThuc());
            existing.setTrangThai(dggDetails.getTrangThai());
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
