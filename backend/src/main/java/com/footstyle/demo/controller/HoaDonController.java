package com.footstyle.demo.controller;

import com.footstyle.demo.dto.HoaDonRequest;
import com.footstyle.demo.service.HoaDonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/hoa-don")
public class HoaDonController {

    @Autowired
    private HoaDonService hoaDonService;

    @GetMapping
    public ResponseEntity<?> getListHoaDon(
            @RequestParam(required = false) Integer trangThai,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("ngayTao").descending());
        Page<HoaDonRequest> responsePage = hoaDonService.getDanhSach(trangThai, pageable);

        
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("content", responsePage.getContent());     
        responseMap.put("totalPages", responsePage.getTotalPages()); 
        responseMap.put("number", responsePage.getNumber());         

        return ResponseEntity.ok(responseMap);
    }
}