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
        if (khachHang.getEmail() != null && !khachHang.getEmail().isEmpty() && khachHangRepository.existsByEmail(khachHang.getEmail())) {
            return ResponseEntity.status(409).body(java.util.Collections.singletonMap("message", "Email đã tồn tại."));
        }
        if (khachHang.getSdt() != null && !khachHang.getSdt().isEmpty() && khachHangRepository.existsBySdt(khachHang.getSdt())) {
            return ResponseEntity.status(409).body(java.util.Collections.singletonMap("message", "Số điện thoại đã tồn tại."));
        }

        if (khachHang.getHoTen() == null || khachHang.getHoTen().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("message", "Vui lòng nhập họ tên khách hàng."));
        }

        if (khachHang.getMaKhachHang() == null || khachHang.getMaKhachHang().isEmpty()) {
            khachHang.setMaKhachHang("KH" + System.currentTimeMillis());
        }

        if (khachHang.getSdt() != null) {
            khachHang.setMatKhau(java.util.UUID.randomUUID().toString().substring(0, 8));
        }

        if (khachHang.getTrangThai() == null) {
            khachHang.setTrangThai(1);
        }

        if (khachHang.getDiaChiList() != null) {
            khachHang.getDiaChiList().forEach(dc -> {
                dc.setKhachHang(khachHang);
                if (dc.getTrangThai() == null) dc.setTrangThai(1);
            });
        }
        
        try {
            return ResponseEntity.ok(khachHangRepository.save(khachHang));
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            String rootMsg = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
            return ResponseEntity.status(409).body(java.util.Collections.singletonMap("message", "Lỗi lưu dữ liệu: " + rootMsg));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateKhachHang(@PathVariable Integer id, @RequestBody KhachHang khachHangDetails) {
        Optional<KhachHang> optionalKhachHang = khachHangRepository.findById(id);
        if (optionalKhachHang.isPresent()) {
            KhachHang khachHang = optionalKhachHang.get();

            if (khachHangDetails.getEmail() != null && !khachHangDetails.getEmail().equals(khachHang.getEmail()) && khachHangRepository.existsByEmail(khachHangDetails.getEmail())) {
                return ResponseEntity.status(409).body(java.util.Collections.singletonMap("message", "Email đã tồn tại."));
            }
            if (khachHangDetails.getSdt() != null && !khachHangDetails.getSdt().equals(khachHang.getSdt()) && khachHangRepository.existsBySdt(khachHangDetails.getSdt())) {
                return ResponseEntity.status(409).body(java.util.Collections.singletonMap("message", "Số điện thoại đã tồn tại."));
            }

            if (khachHangDetails.getHoTen() != null && !khachHangDetails.getHoTen().trim().isEmpty()) {
                khachHang.setHoTen(khachHangDetails.getHoTen().trim());
            }

            khachHang.setSdt(khachHangDetails.getSdt());
            khachHang.setEmail(khachHangDetails.getEmail());
            khachHang.setNgaySinh(khachHangDetails.getNgaySinh());
            khachHang.setGioiTinh(khachHangDetails.getGioiTinh());

            if (khachHangDetails.getTrangThai() != null) {
                khachHang.setTrangThai(khachHangDetails.getTrangThai());
            }
            
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
