package com.footstyle.demo.controller;

import com.footstyle.demo.entity.DiaChiKhachHang;
import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.repository.DiaChiKhachHangRepository;
import com.footstyle.demo.repository.KhachHangRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dia-chi")
@CrossOrigin(originPatterns = "*")
public class DiaChiKhachHangController {

    @Autowired
    private DiaChiKhachHangRepository diaChiRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @GetMapping("/khach-hang/{idKhachHang}")
    public ResponseEntity<?> getDiaChiByKhachHang(@PathVariable Integer idKhachHang) {
        List<DiaChiKhachHang> list = diaChiRepository.findByKhachHangId(idKhachHang);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/khach-hang/{idKhachHang}")
    public ResponseEntity<?> addDiaChi(@PathVariable Integer idKhachHang, @RequestBody DiaChiKhachHang diaChi) {
        return khachHangRepository.findById(idKhachHang).map(kh -> {
            diaChi.setKhachHang(kh);

            diaChi.setNgayTao(LocalDateTime.now());


            if (diaChi.getLaDiaChiMacDinh() != null && diaChi.getLaDiaChiMacDinh()) {
                // Remove default from other addresses
                List<DiaChiKhachHang> others = diaChiRepository.findByKhachHangId(idKhachHang);
                for (DiaChiKhachHang other : others) {
                    other.setLaDiaChiMacDinh(false);
                    diaChiRepository.save(other);
                }
            }
            return ResponseEntity.ok(diaChiRepository.save(diaChi));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDiaChi(@PathVariable Integer id, @RequestBody DiaChiKhachHang payload) {
        return diaChiRepository.findById(id).map(existing -> {
            existing.setTenNguoiNhan(payload.getTenNguoiNhan());
            existing.setSdtNguoiNhan(payload.getSdtNguoiNhan());
            existing.setTinhThanhPho(payload.getTinhThanhPho());
            existing.setQuanHuyen(payload.getQuanHuyen());
            existing.setPhuongXa(payload.getPhuongXa());
            existing.setDiaChiCuThe(payload.getDiaChiCuThe());

            existing.setNgayCapNhat(LocalDateTime.now());

            
            if (payload.getLaDiaChiMacDinh() != null && payload.getLaDiaChiMacDinh()) {
                existing.setLaDiaChiMacDinh(true);
                List<DiaChiKhachHang> others = diaChiRepository.findByKhachHangId(existing.getKhachHang().getId());
                for (DiaChiKhachHang other : others) {
                    if (!other.getId().equals(existing.getId())) {
                        other.setLaDiaChiMacDinh(false);
                        diaChiRepository.save(other);
                    }
                }
            } else {
                existing.setLaDiaChiMacDinh(payload.getLaDiaChiMacDinh());
            }
            
            return ResponseEntity.ok(diaChiRepository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDiaChi(@PathVariable Integer id) {
        if (diaChiRepository.existsById(id)) {
            diaChiRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Xóa thành công"));
        }
        return ResponseEntity.notFound().build();
    }
}
