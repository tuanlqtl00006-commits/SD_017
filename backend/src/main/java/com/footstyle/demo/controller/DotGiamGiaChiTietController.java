package com.footstyle.demo.controller;

import com.footstyle.demo.entity.DotGiamGia;
import com.footstyle.demo.entity.DotGiamGiaChiTiet;
import com.footstyle.demo.repository.DotGiamGiaRepository;
import com.footstyle.demo.repository.DotGiamGiaChiTietRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/dot-giam-gia-chi-tiet")
public class DotGiamGiaChiTietController {

    @Autowired
    private DotGiamGiaChiTietRepository dotGiamGiaChiTietRepository;

    @Autowired
    private DotGiamGiaRepository dotGiamGiaRepository;

    @GetMapping("/campaign/{campaignId}")
    public List<DotGiamGiaChiTiet> getByCampaign(@PathVariable Long campaignId) {
        return dotGiamGiaChiTietRepository.findByDotGiamGiaId(campaignId);
    }

    @PostMapping("/campaign/{campaignId}")
    public ResponseEntity<?> addProductToCampaign(@PathVariable Long campaignId, @RequestBody DotGiamGiaChiTiet chiTiet) {
        Optional<DotGiamGia> campaignOpt = dotGiamGiaRepository.findById(campaignId);
        if (!campaignOpt.isPresent()) {
            return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("message", "KhĂ´ng tĂ¬m tháº¥y Ä‘á»£t giáº£m giĂ¡"));
        }
        
        if (dotGiamGiaChiTietRepository.existsByDotGiamGiaIdAndIdSanPhamChiTiet(campaignId, chiTiet.getIdSanPhamChiTiet())) {
            return ResponseEntity.status(409).body(java.util.Collections.singletonMap("message", "Sáº£n pháº©m nĂ y Ä‘Ă£ tá»“n táº¡i trong Ä‘á»£t giáº£m giĂ¡"));
        }

        chiTiet.setDotGiamGia(campaignOpt.get());
        if(chiTiet.getTrangThai() == null) chiTiet.setTrangThai(1);
        
        return ResponseEntity.ok(dotGiamGiaChiTietRepository.save(chiTiet));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (dotGiamGiaChiTietRepository.existsById(id)) {
            dotGiamGiaChiTietRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
