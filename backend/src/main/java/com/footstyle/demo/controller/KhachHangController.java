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
    public List<KhachHang> getAllKhachHang(@RequestParam(required = false) String search) {
        if (search != null && !search.trim().isEmpty()) {
            return khachHangRepository.searchKhachHang(search);
        }
        return khachHangRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<KhachHang> getKhachHangById(@PathVariable Long id) {
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

        if (khachHang.getMaKH() == null || khachHang.getMaKH().isEmpty()) {
            khachHang.setMaKH("KH" + System.currentTimeMillis());
        }
        
        
        if (khachHang.getSdt() != null) {
            khachHang.setMatKhau(khachHang.getSdt());
        }
        
        
        if (khachHang.getTinhThanh() != null && !khachHang.getTinhThanh().isEmpty()) {
            com.footstyle.demo.entity.DiaChiKhachHang diaChi = new com.footstyle.demo.entity.DiaChiKhachHang();
            diaChi.setTinhThanhPho(khachHang.getTinhThanh());
            diaChi.setQuanHuyen(khachHang.getQuanHuyen());
            diaChi.setPhuongXa(khachHang.getPhuongXa());
            diaChi.setDiaChiCuThe(khachHang.getDiaChiCuThe());
            diaChi.setTenNguoiNhan(khachHang.getTen());
            diaChi.setSdtNguoiNhan(khachHang.getSdt());
            diaChi.setLaDiaChiMacDinh(true);
            diaChi.setTrangThai(1);
            diaChi.setKhachHang(khachHang);
            khachHang.getDiaChiList().add(diaChi);
        }

        return ResponseEntity.ok(khachHangRepository.save(khachHang));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateKhachHang(@PathVariable Long id, @RequestBody KhachHang khachHangDetails) {
        Optional<KhachHang> optionalKhachHang = khachHangRepository.findById(id);
        if (optionalKhachHang.isPresent()) {
            KhachHang khachHang = optionalKhachHang.get();

            
            if (khachHangDetails.getEmail() != null && !khachHangDetails.getEmail().equals(khachHang.getEmail()) && khachHangRepository.existsByEmail(khachHangDetails.getEmail())) {
                return ResponseEntity.status(409).body(java.util.Collections.singletonMap("message", "Email đã tồn tại."));
            }
            
            if (khachHangDetails.getSdt() != null && !khachHangDetails.getSdt().equals(khachHang.getSdt()) && khachHangRepository.existsBySdt(khachHangDetails.getSdt())) {
                return ResponseEntity.status(409).body(java.util.Collections.singletonMap("message", "Số điện thoại đã tồn tại."));
            }

            khachHang.setTen(khachHangDetails.getTen());
            khachHang.setSdt(khachHangDetails.getSdt());
            khachHang.setEmail(khachHangDetails.getEmail());
            khachHang.setNgaySinh(khachHangDetails.getNgaySinh());
            khachHang.setGioiTinh(khachHangDetails.getGioiTinh());
            khachHang.setTrangThai(khachHangDetails.getTrangThai());
            
            
            if (khachHangDetails.getTinhThanh() != null && !khachHangDetails.getTinhThanh().isEmpty()) {
                com.footstyle.demo.entity.DiaChiKhachHang diaChi;
                if (!khachHang.getDiaChiList().isEmpty()) {
                    diaChi = khachHang.getDiaChiList().get(0);
                } else {
                    diaChi = new com.footstyle.demo.entity.DiaChiKhachHang();
                    diaChi.setKhachHang(khachHang);
                    diaChi.setLaDiaChiMacDinh(true);
                    diaChi.setTrangThai(1);
                    khachHang.getDiaChiList().add(diaChi);
                }
                diaChi.setTinhThanhPho(khachHangDetails.getTinhThanh());
                diaChi.setQuanHuyen(khachHangDetails.getQuanHuyen());
                diaChi.setPhuongXa(khachHangDetails.getPhuongXa());
                diaChi.setDiaChiCuThe(khachHangDetails.getDiaChiCuThe());
                diaChi.setTenNguoiNhan(khachHangDetails.getTen());
                diaChi.setSdtNguoiNhan(khachHangDetails.getSdt());
            }

            return ResponseEntity.ok(khachHangRepository.save(khachHang));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteKhachHang(@PathVariable Long id) {
        Optional<KhachHang> khachHang = khachHangRepository.findById(id);
        if (khachHang.isPresent()) {
            khachHangRepository.delete(khachHang.get());
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}



