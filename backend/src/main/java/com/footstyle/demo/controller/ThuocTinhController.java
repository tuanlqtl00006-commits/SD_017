package com.footstyle.demo.controller;

import com.footstyle.demo.dto.ThuocTinhRequest;
import com.footstyle.demo.dto.ThuocTinhResponse;
import com.footstyle.demo.service.ThuocTinhService;
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
@RequestMapping("/api/thuoc-tinh/{loai}")
@RequiredArgsConstructor
public class ThuocTinhController {

    private final ThuocTinhService service;

    @GetMapping
    public List<ThuocTinhResponse> getAll(@PathVariable String loai) {
        return service.getAll(loai);
    }

    @PostMapping
    public ResponseEntity<ThuocTinhResponse> them(@PathVariable String loai, @RequestBody ThuocTinhRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.them(loai, req));
    }

    @PutMapping("/{id}")
    public ThuocTinhResponse sua(@PathVariable String loai, @PathVariable Integer id, @RequestBody ThuocTinhRequest req) {
        return service.sua(loai, id, req);
    }

    
    @PutMapping("/{id}/trang-thai")
    public ThuocTinhResponse doiTrangThai(@PathVariable String loai, @PathVariable Integer id) {
        return service.doiTrangThai(loai, id);
    }
}
