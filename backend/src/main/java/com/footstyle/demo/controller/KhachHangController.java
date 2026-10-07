package com.footstyle.demo.controller;

import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.repository.KhachHangRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/khach-hang")
public class KhachHangController {

    @Autowired
    private KhachHangRepository khachHangRepository;

    @GetMapping
    public List<KhachHang> getAllKhachHang() {
        return khachHangRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "id"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<KhachHang> getKhachHangById(@PathVariable Integer id) {
        Optional<KhachHang> khachHang = khachHangRepository.findById(id);
        return khachHang.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createKhachHang(@RequestBody KhachHang khachHang) {
        if (khachHang.getMaKhachHang() == null || khachHang.getMaKhachHang().isEmpty()) {
            khachHang.setMaKhachHang("KH" + System.currentTimeMillis());
        }
        
        return ResponseEntity.ok(khachHangRepository.save(khachHang));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateKhachHang(@PathVariable Integer id, @RequestBody KhachHang khachHangDetails) {
        Optional<KhachHang> optionalKhachHang = khachHangRepository.findById(id);
        if (optionalKhachHang.isPresent()) {
            KhachHang khachHang = optionalKhachHang.get();
            khachHang.setHoTen(khachHangDetails.getHoTen());
            khachHang.setSdt(khachHangDetails.getSdt());
            khachHang.setEmail(khachHangDetails.getEmail());
            khachHang.setTrangThai(khachHangDetails.getTrangThai());
            
            return ResponseEntity.ok(khachHangRepository.save(khachHang));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteKhachHang(@PathVariable Integer id) {
        Optional<KhachHang> khachHang = khachHangRepository.findById(id);
        if (khachHang.isPresent()) {
            khachHangRepository.delete(khachHang.get());
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
